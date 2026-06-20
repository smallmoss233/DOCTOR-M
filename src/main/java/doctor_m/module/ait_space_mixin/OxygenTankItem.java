package doctor_m.module.ait_space_mixin;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class OxygenTankItem extends Item {
    public static final String OXYGEN_KEY = "doctor_m_oxygen";
    public static final double MAX_OXYGEN = 1200.0;
    private static final double TRANSFER_RATE = 100; // 每次转移100点

    public OxygenTankItem(Settings settings) {
        super(settings);
    }

    public static double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    public static void setOxygen(ItemStack stack, double amount) {
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, MAX_OXYGEN));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return TypedActionResult.fail(user.getStackInHand(hand));

        ItemStack tankStack = user.getStackInHand(hand);
        ItemStack chestStack = user.getInventory().armor.get(2);

        if (!(chestStack.getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem)) {
            user.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.no_suit"), true);
            return TypedActionResult.fail(tankStack);
        }

        double tankOxygen = getOxygen(tankStack);
        double suitOxygen = SpaceOxygenManager.getOxygen(chestStack);

        if (tankOxygen <= 0) {
            user.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.empty"), true);
            return TypedActionResult.fail(tankStack);
        }

        if (suitOxygen >= SpaceOxygenManager.MAX_OXYGEN) {
            user.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.suit_full"), true);
            return TypedActionResult.fail(tankStack);
        }

        double transferAmount = Math.min(TRANSFER_RATE, tankOxygen);
        transferAmount = Math.min(transferAmount, SpaceOxygenManager.MAX_OXYGEN - suitOxygen);

        SpaceOxygenManager.refillOxygen(chestStack, transferAmount);
        setOxygen(tankStack, tankOxygen - transferAmount);

        user.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.transfer", transferAmount), true);

        if (getOxygen(tankStack) <= 0) {
            user.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.depleted"), true);
        }

        user.getItemCooldownManager().set(this, 5);
        return TypedActionResult.success(tankStack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        double oxygen = getOxygen(stack);
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, MAX_OXYGEN));
    }
}