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
        cir.setReturnValue(false);
    }

    // 新增：劫持 isUnsafeBranch，返回 false 以禁用警告框
    @Inject(method = "isUnsafeBranch", at = @At("HEAD"), cancellable = true)
    private static void onIsUnsafeBranch(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}