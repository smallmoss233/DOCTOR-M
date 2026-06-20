package com.example.doctor_m.mixin;

import dev.amble.ait.core.devteam.BetaVerification;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BetaVerification.class)
public class MixinBetaVerification {

    /**
     * 阻止 init() 启动验证服务
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private static void onInit(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * 让 isServerRunning() 永远返回 false
     */
    @Inject(method = "isServerRunning", at = @At("HEAD"), cancellable = true)
    private static void onIsServerRunning(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}