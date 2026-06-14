package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.TrinketItem;
import doctor_m.util.ShiftTooltipInvoker;
import doctor_m.util.TooltipHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import dev.emi.trinkets.api.SlotReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.client.item.TooltipContext;
import java.util.List;

public class time_key extends TrinketItem {

    public time_key(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 读取 NBT 状态
        NbtCompound nbt = stack.getNbt();
        boolean neutral = nbt != null && nbt.getBoolean("neutral_mode");
        boolean godmode = nbt != null && nbt.getBoolean("godmode");

        // 显示状态（灰色，始终显示）
        tooltip.add(Text.translatable("message.doctor_m.time_key.neutral_status",
                        neutral ? Text.translatable("key.doctor_m.mode.on") : Text.translatable("key.doctor_m.mode.off"))
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("message.doctor_m.time_key.godmode_status",
                        godmode ? Text.translatable("key.doctor_m.mode.on") : Text.translatable("key.doctor_m.mode.off"))
                .formatted(Formatting.GRAY));

        // 长文本
        Text longDescription = Text.translatable("message.doctor_m.time_key.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        // 未完成提示
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
        // 潜行详情提示
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.time_key.detail")
        );
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            // 开启飞行能力
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
            // 关闭飞行能力（除非是创造模式）
            if (!player.isCreative() && player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
            }
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        long time = System.currentTimeMillis();
        float period = 8000f; // 完整周期 8 秒（从白到紫再回白）
        float phase = (time % (long) period) / period * (float) (2 * Math.PI);
        float r = 0.5f + 0.5f * (float) Math.cos(phase);
        float g = 0.5f + 0.5f * (float) Math.cos(phase + Math.PI);
        float b = 0.5f + 0.5f * (float) Math.cos(phase + Math.PI/2);

        // 组合成 RGB 整数
        int color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);

        Text baseName = super.getName(stack);
        return baseName.copy().styled(style -> style.withColor(TextColor.fromRgb(color)));
    }
}