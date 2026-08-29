package doctor_m.mixin.doctor_m;

import doctor_m.api.IEntityDataSaver;
import doctor_m.api.UndyingHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityHealthMigrationMixin {

    private static final String KEY = "ktt_hidden_health";

    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
    private void onGetHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity player && UndyingHelper.hasKTT(player)) {
            if (self.getWorld().isClient) {
                cir.setReturnValue(20.0f);
            } else {
                NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
                if (!data.contains(KEY)) {
                    data.putFloat(KEY, Float.POSITIVE_INFINITY);
                }
                cir.setReturnValue(data.getFloat(KEY));
            }
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float value, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity player && UndyingHelper.hasKTT(player)) {
            NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
            data.putFloat(KEY, value);
            ci.cancel();
        }
    }
}