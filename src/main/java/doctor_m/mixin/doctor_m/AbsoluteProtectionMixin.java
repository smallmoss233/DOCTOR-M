package doctor_m.mixin.doctor_m;

import doctor_m.handler.KeytoTime.HealthGuard;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.handler.KeytoTime.KeytoTimePassive;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
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
                if (KeytoTimePassive.isGodMode(player)) {
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
                if (KeytoTimePassive.isGodMode(player)) {
                    ci.cancel();
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockKill(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (KeytoTimeCore.isTimeKeyEquipped(player)) {
                    player.setHealth(player.getMaxHealth());
                    KeytoTimeCore.onDeathIntercepted(player);
                    ci.cancel();
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockOnDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (KeytoTimeCore.isTimeKeyEquipped(player)) {
                    KeytoTimeCore.revivePlayer(player);
                    ci.cancel();
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void doctor_m$tickResurrection(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            try {
                if (KeytoTimeCore.isTimeKeyEquipped(player)) {
                    if (player.getHealth() <= 0.0f || player.deathTime > 0) {
                        KeytoTimeCore.revivePlayer(player);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * 时间钥匙防修改血量机制
     */
    @Inject(method = "setHealth(F)V", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (!KeytoTimeCore.isTimeKeyEquipped(player)) return;
        if (HealthGuard.isHealthWriteAllowed()) return;

        ci.cancel();
    }

    @Inject(
            method = "getAttributeInstance(Lnet/minecraft/entity/attribute/EntityAttribute;)Lnet/minecraft/entity/attribute/EntityAttributeInstance;",
            at = @At("HEAD")
    )
    private void beforeGetAttributeInstance(EntityAttribute attribute, CallbackInfoReturnable<EntityAttributeInstance> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        HealthGuard.attributeAccessEntity.set(entity);
    }
}