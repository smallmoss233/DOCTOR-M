package doctor_m.mixin.doctor_m;

import doctor_m.api.UndyingHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityDeathInsuranceMixin {

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && UndyingHelper.hasKTT(player)) {
            ci.cancel();
        }
    }
}