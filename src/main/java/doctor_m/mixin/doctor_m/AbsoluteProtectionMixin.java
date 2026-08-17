package doctor_m.mixin.doctor_m;

import doctor_m.handler.TimeKey.TimeKeyFunction;
import doctor_m.handler.TimeKey.TimeKeyPassive;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class AbsoluteProtectionMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockGodModeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (TimeKeyPassive.isGodMode(player)) {
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                    player.setFireTicks(0);
                    player.setOnFire(false);
                    cir.setReturnValue(false);
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockGodModeKnockback(double strength, double x, double z, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (TimeKeyPassive.isGodMode(player)) {
                    ci.cancel();
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockKill(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                    player.setHealth(player.getMaxHealth());
                    TimeKeyFunction.onDeathIntercepted(player);
                    ci.cancel();
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockOnDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                    TimeKeyFunction.revivePlayer(player);
                    ci.cancel();
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void doctor_m$tickResurrection(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                    if (player.getHealth() <= 0.0f || player.deathTime > 0) {
                        TimeKeyFunction.revivePlayer(player);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}