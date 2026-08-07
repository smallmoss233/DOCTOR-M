package doctor_m.mixin.client.doctor_m;

import doctor_m.Item.data_itme.ToymakerHammerItem;
import doctor_m.module.creativity.creativity_data.Tlipoca.TlipocaScytheItem;
import doctor_m.util.creativity.ScytheChargingManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void doctor_m$scytheZoom(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        if (!player.isUsingItem()) return;

        ItemStack activeStack = player.getActiveItem();
        float base = cir.getReturnValue();

        // 镰刀 FOV 缩放
        if (activeStack.getItem() instanceof TlipocaScytheItem) {
            int useTicks = player.getItemUseTime();
            float maxTicks = ScytheChargingManager.MAX_CHARGE_LEVEL * ScytheChargingManager.TICKS_PER_LEVEL;
            float progress = Math.min(useTicks / maxTicks, 1.0f);
            float eased = progress * progress * (3.0f - 2.0f * progress);
            cir.setReturnValue(base * (1.0f - eased * 0.45f));
        }
        // 玩具匠锤子 FOV 缩放
        else if (activeStack.getItem() instanceof ToymakerHammerItem hammer) {
            int useTicks = player.getItemUseTime();
            float maxTicks = hammer.getMaxUseTime(activeStack);
            float progress = Math.min(useTicks / maxTicks, 1.0f);
            float eased = progress * progress * (3.0f - 2.0f * progress);
            // 满蓄缩小到 50% FOV（视野放大约 2 倍），比镰刀更夸张一点
            cir.setReturnValue(base * (1.0f - eased * 0.50f));
        }
    }
}