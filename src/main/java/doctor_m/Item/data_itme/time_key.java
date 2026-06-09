package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.TrinketItem;
import doctor_m.util.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import dev.emi.trinkets.api.SlotReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class time_key extends TrinketItem {

    public time_key(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 从语言文件读取需要自动换行的长文本
        Text longDescription = Text.translatable("txt.doctor_m.time_key.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription, 30);
        // 可继续添加其他固定行
        tooltip.add(Text.translatable("txt.doctor_m.tip.not.done"));
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
}