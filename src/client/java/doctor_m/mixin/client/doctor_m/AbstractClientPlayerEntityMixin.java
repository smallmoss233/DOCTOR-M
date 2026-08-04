package doctor_m.mixin.client.doctor_m;

import doctor_m.module.creativity.creativity_data.TlipocaScytheItem;
import doctor_m.util.creativity.ScytheChargingManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void doctor_m$scytheZoom(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        if (player.isUsingItem() && player.getActiveItem().getItem() instanceof TlipocaScytheItem) {
            float base = cir.getReturnValue();
            int useTicks = player.getItemUseTime();
            float maxTicks = ScytheChargingManager.MAX_CHARGE_LEVEL * ScytheChargingManager.TICKS_PER_LEVEL;
            float progress = Math.min(useTicks / maxTicks, 1.0f);
            // smoothstep 缓动
            float eased = progress * progress * (3.0f - 2.0f * progress);
            // 满蓄缩小到 55% FOV（视野放大约 1.8 倍）
            cir.setReturnValue(base * (1.0f - eased * 0.45f));
        }
    }
}