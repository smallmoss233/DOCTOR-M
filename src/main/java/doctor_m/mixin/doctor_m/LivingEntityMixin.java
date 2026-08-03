package doctor_m.mixin.doctor_m;

import doctor_m.Item.data_itme.ForceFieldShieldItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * 完全免疫非环境/特殊伤害
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$onShieldDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;
        if (!ForceFieldShieldItem.isForceFieldActive(player)) return;

        // 环境/特殊伤害：90% 减伤
        if (ForceFieldShieldItem.isEnvironmentalOrSpecialDamage(source)) {
            return;
        }

        // 物理、弹射物、爆炸、监守者音波等完全免疫
        cir.setReturnValue(false);
    }

    /**
     * 环境/特殊伤害减伤 90%（兜底保护）
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$reduceEnvironmentalDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return amount;
        if (!ForceFieldShieldItem.isForceFieldActive(player)) return amount;

        if (ForceFieldShieldItem.isEnvironmentalOrSpecialDamage(source)) {
            return amount * 0.1f; // 只受 10% 伤害
        }
        return amount;
    }
}