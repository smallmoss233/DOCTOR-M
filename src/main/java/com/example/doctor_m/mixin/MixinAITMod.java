package com.example.doctor_m.mixin;

import dev.amble.ait.AITMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AITMod.class)
public class MixinAITMod {

    @Inject(method = "isBetaLocked", at = @At("HEAD"), cancellable = true)
    private static void onIsBetaLocked(CallbackInfoReturnable<Boolean> cir) {
        // 永远返回 false，禁用 Beta 锁定
        cir.setReturnValue(false);
    }
}