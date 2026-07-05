package com.example.doctor_m.mixin.aitmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.amble.ait.core.tardis.handler.ShieldHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShieldHandler.class)
public class MixinShieldHandler {

    // 扫描范围扩大到 8 格（配合下面的 8 格立方体）
    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Box;expand(D)Lnet/minecraft/util/math/Box;"
            ),
            index = 0
    )
    private double modifyShieldScanRange(double value) {
        return 8.0;
    }

    /**
     * 把球体边界改成轴对齐立方体边界
     * 原来：squaredDistanceTo(center) <= 8  →  半径 2.8 格的球
     * 现在：每个轴独立比较 |diff| <= 4  →  8×8×8 的立方体
     *
     * 注意：这个 WrapOperation 会匹配 lambda$tick$1 里的 squaredDistanceTo 调用
     * 需要确认 method 名是否正确
     */
    @WrapOperation(
            method = "lambda$tick$1",  // 或 "tick"，如果 MixinExtras 能穿透 lambda
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D"
            )
    )
    private double wrapSquaredDistanceTo(Entity entity, Vec3d center, Operation<Double> original) {
        Vec3d diff = entity.getPos().subtract(center);
        double maxAxis = Math.max(Math.abs(diff.x), Math.max(Math.abs(diff.y), Math.abs(diff.z)));
        // 返回一个"伪距离平方"：如果 maxAxis <= 4，返回 0（表示在范围内）
        // 如果 > 4，返回一个很大的数（表示在范围外）
        // 这样外层 <= 8.0 的比较就能正常工作
        return maxAxis <= 4.0 ? 0.0 : 9999.0;
    }
}