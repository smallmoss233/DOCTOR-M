package doctor_m.Item.data_item;

import doctor_m.handler.BreathingMachineHandler;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class BreathingMachineItem extends Item {
    public static final String ACTIVE_KEY = "active";
    public static final String ENERGY_KEY = "energy";
    public static final String TIME_LEFT_KEY = "time_left";
    public static final int MAX_TIME = 3600;
    public static final int MAX_ENERGY = 180;
    public static final int FOOD_COST = 1;
    public static final int ENERGY_PER_USE = 5;
    public static final int MIN_FOOD = 7;

    public BreathingMachineItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = stack.getOrCreateNbt();

        if (nbt.getBoolean(ACTIVE_KEY)) {
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
                BreathingMachineHandler.triggerFatalEffect(serverPlayer, stack, false);
            }
            return TypedActionResult.success(stack);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        int energy = nbt != null ? nbt.getInt(ENERGY_KEY) : MAX_ENERGY;
        boolean active = nbt != null && nbt.getBoolean(ACTIVE_KEY);
        int timeLeft = nbt != null ? nbt.getInt(TIME_LEFT_KEY) : MAX_TIME;

        tooltip.add(Text.literal("电量: " + energy + " / " + MAX_ENERGY).formatted(Formatting.GREEN));
        if (active) {
            tooltip.add(Text.literal("状态: 正在运行 (" + (timeLeft / 20) + "s)").formatted(Formatting.RED));
        } else {
            tooltip.add(Text.literal("状态: 待机").formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}