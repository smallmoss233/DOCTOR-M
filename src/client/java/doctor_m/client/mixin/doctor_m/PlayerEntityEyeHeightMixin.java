package doctor_m.client.mixin.doctor_m;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityEyeHeightMixin {

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void onGetActiveEyeHeight(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (pose == EntityPose.SWIMMING) {
            cir.setReturnValue(1.2F);
        }
    }
}