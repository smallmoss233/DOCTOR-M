package com.example.doctor_m.mixin.aitmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.core.tardis.handler.ShieldHandler;
import dev.amble.ait.core.tardis.control.impl.SecurityControl;
import dev.amble.ait.data.Loyalty;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldHandler.class)
public abstract class MixinShieldHandler {

    // --- 原有的档位/范围修改（保持不变） ---
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

    @WrapOperation(
            method = "lambda$tick$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;squaredDistanceTo(Lnet/minecraft/util/math/Vec3d;)D"
            )
    )
    private double wrapSquaredDistanceTo(Entity entity, Vec3d center, Operation<Double> original) {
        Vec3d diff = entity.getPos().subtract(center);
        double maxAxis = Math.max(Math.abs(diff.x), Math.max(Math.abs(diff.y), Math.abs(diff.z)));
        return maxAxis <= 4.0 ? 0.0 : 9999.0;
    }

    // --- 新增：为忠诚玩家添加增益效果 ---
    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void onTickTail(MinecraftServer server, CallbackInfo ci) {
        // 强制转换为 ShieldHandler 以访问其字段和方法（this 本身就是）
        ShieldHandler self = (ShieldHandler) (Object) this;

        // 护盾必须开启且子系统正常
        if (!self.shielded().get() || !self.tardis().subsystems().shields().isEnabled()
                || self.tardis().subsystems().shields().isBroken()) {
            return;
        }

        // 获取外部位置
        var travel = self.tardis().travel();
        var globalPos = travel.position();
        World world = globalPos.getWorld();
        BlockPos exteriorPos = globalPos.getPos();

        // 扫描护盾范围内的所有玩家
        world.getOtherEntities(null, new Box(exteriorPos).expand(4.0))
                .stream()
                .filter(entity -> entity instanceof ServerPlayerEntity)
                .map(entity -> (ServerPlayerEntity) entity)
                .forEach(player -> {
                    // 判断是否为"忠诚"玩家：COMPANION 或持有匹配钥匙
                    boolean isLoyal = self.tardis().loyalty().get(player).isOf(Loyalty.Type.COMPANION)
                            || SecurityControl.hasMatchingKey(player, self.tardis());

                    if (!isLoyal) return;

                    // 1. 给予生命恢复效果（每秒恢复 1 心，持续 1 秒，每 tick 刷新）
                    player.addStatusEffect(
                            new StatusEffectInstance(StatusEffects.REGENERATION, 20, 0, true, false, false)
                    );
                    player.addStatusEffect(
                            new StatusEffectInstance(AITStatusEffects.OXYGENATED, 60, 0, false, false)
                    );
                        // 重置氧气条为最大值（立即补满）
                        player.setAir(player.getMaxAir());

                });
    }
}