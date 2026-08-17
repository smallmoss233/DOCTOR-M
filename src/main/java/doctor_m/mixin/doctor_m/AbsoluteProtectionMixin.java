package doctor_m.mixin.doctor_m;

import doctor_m.handler.TimeKey.TimeKeyFunction;
import doctor_m.handler.TimeKey.TimeKeyPassive;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class AbsoluteProtectionMixin {

    private boolean doctor_m$isPlayerReady(ServerPlayerEntity player) {
        try {
            java.lang.reflect.Field f = net.minecraft.entity.player.PlayerEntity.class.getDeclaredField("inventory");
            f.setAccessible(true);
            return f.get(player) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockGodModeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && doctor_m$isPlayerReady(player)) {
            if (TimeKeyPassive.isGodMode(player)) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
                player.setFireTicks(0);
                player.setOnFire(false);
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockGodModeKnockback(double strength, double x, double z, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && doctor_m$isPlayerReady(player)) {
            if (TimeKeyPassive.isGodMode(player)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockKill(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && doctor_m$isPlayerReady(player)) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                player.setHealth(player.getMaxHealth());
                TimeKeyFunction.onDeathIntercepted(player);
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$blockHealthSet(float health) {
        if ((Object) this instanceof ServerPlayerEntity player && doctor_m$isPlayerReady(player)) {
            if (TimeKeyPassive.isGodMode(player)) {
                return player.getMaxHealth();
            }
            if (health <= 0.0f && TimeKeyFunction.isTimeKeyEquipped(player)) {
                TimeKeyFunction.onDeathIntercepted(player);
                try {
                    LivingEntity.class.getDeclaredField("deathTime").setInt(player, 0);
                    LivingEntity.class.getDeclaredField("hurtTime").setInt(player, 0);
                } catch (Exception ignored) {}
                return player.getMaxHealth();
            }
        }
        return health;
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockOnDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && doctor_m$isPlayerReady(player)) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                TimeKeyFunction.revivePlayer(player);
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void doctor_m$tickResurrection(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && doctor_m$isPlayerReady(player)) {
            if (player.getWorld() == null) return;

            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                if (player.getHealth() <= 0.0f || player.deathTime > 0) {
                    TimeKeyFunction.revivePlayer(player);
                }
            }
        }
    }
}