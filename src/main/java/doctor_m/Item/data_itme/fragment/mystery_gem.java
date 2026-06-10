package doctor_m.Item.data_itme.fragment;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class mystery_gem extends TrinketItem {

    public mystery_gem(Settings settings) {
        super(settings);
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            // 给予抗性 II（40% 减伤），无限时长，不显示粒子
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE,
                    Integer.MAX_VALUE,
                    1,
                    false,
                    false,
                    false
            ));
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onUnequip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            // 移除抗性效果
            player.removeStatusEffect(StatusEffects.RESISTANCE);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.world.World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.translatable("message.doctor_m.mystery_gem").formatted(Formatting.GRAY));
    }
}