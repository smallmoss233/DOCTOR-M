package com.example.doctor_m.mixin.aitmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.amble.ait.core.tardis.handler.ShieldHandler;
import doctor_m.util.config.ConfigManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShieldHandler.class)
public class MixinShieldHandler {

    // 修改 Box.expand 的参数为配置的半边长
    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Box;expand(D)Lnet/minecraft/util/math/Box;"
            ),
            index = 0
    )
    private double modifyShieldScanRange(double value) {
        return ConfigManager.getConfig().shieldHalfSize;
    }

    /**
     * 将球体检测改为立方体检测，使用配置的半边长
     */
    @WrapOperation(
            method = "lambda$tick$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D"
            )
    )
    private double wrapSquaredDistanceTo(Entity entity, Vec3d center, Operation<Double> original) {
        double halfSize = ConfigManager.getConfig().shieldHalfSize;
        Vec3d diff = entity.getPos().subtract(center);
        double maxAxis = Math.max(Math.abs(diff.x), Math.max(Math.abs(diff.y), Math.abs(diff.z)));
        return maxAxis <= halfSize ? 0.0 : 9999.0;
    }
}