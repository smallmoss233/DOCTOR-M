package doctor_m.mixin.doctor_m;

import doctor_m.handler.KeytoTime.HealthGuard;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityAttributeInstance.class)
public abstract class EntityAttributeInstanceMixin {

    @Shadow
    public abstract EntityAttribute getAttribute();

    private boolean isMaxHealth() {
        return getAttribute() == EntityAttributes.GENERIC_MAX_HEALTH;
    }

    private boolean shouldBlock() {
        LivingEntity entity = HealthGuard.attributeAccessEntity.get();
        HealthGuard.attributeAccessEntity.remove();

        if (entity == null) return false;
        if (!(entity instanceof ServerPlayerEntity player)) return false;
        return KeytoTimeCore.isTimeKeyEquipped(player);
    }

    @Inject(method = "setBaseValue(D)V", at = @At("HEAD"), cancellable = true)
    private void onSetBaseValue(double value, CallbackInfo ci) {
        if (!isMaxHealth()) return;
        if (!shouldBlock()) return;
        if (HealthGuard.isMaxHealthWriteAllowed()) return;

        ci.cancel();
    }

    @Inject(method = "addTemporaryModifier", at = @At("HEAD"), cancellable = true)
    private void onAddTemporaryModifier(CallbackInfo ci) {
        if (!isMaxHealth()) return;
        if (!shouldBlock()) return;
        if (HealthGuard.isMaxHealthWriteAllowed()) return;

        ci.cancel();
    }

    @Inject(method = "addPersistentModifier", at = @At("HEAD"), cancellable = true)
    private void onAddPersistentModifier(CallbackInfo ci) {
        if (!isMaxHealth()) return;
        if (!shouldBlock()) return;
        if (HealthGuard.isMaxHealthWriteAllowed()) return;

        ci.cancel();
    }

    @Inject(method = "removeModifier(Ljava/util/UUID;)V", at = @At("HEAD"), cancellable = true)
    private void onRemoveModifierByUuid(java.util.UUID uuid, CallbackInfo ci) {
        if (!isMaxHealth()) return;
        if (!shouldBlock()) return;
        if (HealthGuard.isMaxHealthWriteAllowed()) return;

        ci.cancel();
    }
}