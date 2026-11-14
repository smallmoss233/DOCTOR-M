package doctor_m.Item.data_item;

import doctor_m.handler.BreathingMachineHandler;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
    public static final int MAX_TIME = 3600; // 3分钟
    public static final int MAX_ENERGY = 180; // 3分钟，每秒扣1点

    public BreathingMachineItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = stack.getOrCreateNbt();

        if (nbt.getBoolean(ACTIVE_KEY)) {
            if (!world.isClient && user instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                // 提前手动结束，触发必死
                BreathingMachineHandler.triggerFatalEffect(serverPlayer, stack, false);
            }
            return TypedActionResult.success(stack);
        }
         else {
            // 未激活状态，右键充电（假设每次右键用红石充能，你可以改成其他逻辑）
            if (!world.isClient) {
                int currentEnergy = nbt.getInt(ENERGY_KEY);
                if (currentEnergy < MAX_ENERGY) {
                    if (user.getOffHandStack().isOf(net.minecraft.item.Items.REDSTONE) || user.getMainHandStack().isOf(net.minecraft.item.Items.REDSTONE)) {
                        // 如果有红石，就充满（你可以改成消耗红石）
                        nbt.putInt(ENERGY_KEY, MAX_ENERGY);
                    }
                }
            }
            return TypedActionResult.success(stack);
        }
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