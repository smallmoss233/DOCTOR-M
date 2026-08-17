package doctor_m.mixin.doctor_m;

import doctor_m.handler.TimeKey.TimeKeyFunction;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityRemovalProtectionMixin {

    @Inject(method = "discard", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockDiscard(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                TimeKeyFunction.revivePlayer(player);
                ci.cancel();
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                TimeKeyFunction.revivePlayer(player);
                ci.cancel();
            }
        }
    }
}