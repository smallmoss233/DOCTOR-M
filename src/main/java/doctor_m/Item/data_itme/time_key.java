package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.TrinketItem;
import doctor_m.util.DynamicColorHelper;
import doctor_m.util.ShiftTooltipInvoker;
import doctor_m.util.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import dev.emi.trinkets.api.SlotReference;

import java.awt.Color;
import java.util.List;

public class time_key extends TrinketItem {

    public time_key(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        boolean neutral = nbt != null && nbt.getBoolean("neutral_mode");
        boolean godmode = nbt != null && nbt.getBoolean("godmode");

        tooltip.add(Text.translatable("message.doctor_m.time_key.neutral_status",
                        neutral ? Text.translatable("key.doctor_m.mode.on") : Text.translatable("key.doctor_m.mode.off"))
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("message.doctor_m.time_key.godmode_status",
                        godmode ? Text.translatable("key.doctor_m.mode.on") : Text.translatable("key.doctor_m.mode.off"))
                .formatted(Formatting.GRAY));

        Text longDescription = Text.translatable("message.doctor_m.time_key.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
        //实验性[渐变详情文本]
        Text detailText = DynamicColorHelper.applyColorCycle(
                Text.translatable("message.doctor_m.time_key.detail"),
                List.of(
                        new Color(128, 0, 128),  // 紫色
                        Color.WHITE
                ),
                8000
        );
        ShiftTooltipInvoker.addShiftTooltip(tooltip, detailText);
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            if (!player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = true;
                player.sendAbilitiesUpdate();
            }
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onUnequip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            if (!player.isCreative() && player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
            }
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(128, 0, 128),  // 紫色
                Color.WHITE,             // 白色
                Color.RED,                // 红色
                Color.WHITE             // 白色
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 8000);
    }
}