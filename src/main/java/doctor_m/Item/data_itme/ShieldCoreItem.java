package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class ShieldCoreItem extends TrinketItem {
    private static final int MAX_ENERGY = 1000;
    private static final int RECHARGE_RATE = 10;
    private static final int COST_PER_DAMAGE = 10;

    public ShieldCoreItem(Settings settings) {
        super(settings);
    }

    // 物品在普通背包栏时的 tick（Item 类提供）
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof PlayerEntity)) return;
        recharge(stack);
    }

    // 物品装备在 Trinket 饰品槽时的 tick（Trinket 接口提供）
    // 修复：参数类型改为 SlotReference，且顺序要正确
    @Override
    public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
        if (!entity.getWorld().isClient && entity instanceof PlayerEntity) {
            recharge(stack);
        }
    }

    private void recharge(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int energy = nbt.getInt("Energy");
        if (energy < MAX_ENERGY) {
            energy = Math.min(energy + 1, MAX_ENERGY);
            nbt.putInt("Energy", energy);
        }
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrCreateNbt().getInt("Energy");
    }

    public static boolean consumeEnergy(ItemStack stack, int amount) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int energy = nbt.getInt("Energy");
        if (energy >= amount) {
            nbt.putInt("Energy", energy - amount);
            return true;
        }
        return false;
    }

    public static int getMaxEnergy() {
        return MAX_ENERGY;
    }

    public static int getCostPerDamage() {
        return COST_PER_DAMAGE;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            ItemStack stack = user.getStackInHand(hand);
            int energy = getEnergy(stack);
            user.sendMessage(Text.literal("护盾能量: " + energy + "/" + MAX_ENERGY), true);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int energy = getEnergy(stack);
        tooltip.add(Text.literal("能量: " + energy + "/" + MAX_ENERGY).formatted(Formatting.GRAY));
    }
}