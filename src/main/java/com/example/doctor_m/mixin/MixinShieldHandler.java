package com.example.doctor_m.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.amble.ait.core.tardis.handler.ShieldHandler;
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

    // --- 修改扫描范围为8格（保持原有） ---
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

    // --- 将球体边界改为立方体（保持原有） ---
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

    // --- 新增：为护盾内的忠诚玩家提供增益（生命恢复随等级提升） ---
    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void onTickTail(MinecraftServer server, CallbackInfo ci) {
        ShieldHandler self = (ShieldHandler) (Object) this;

        if (!self.shielded().get() || !self.tardis().subsystems().shields().isEnabled()
                || self.tardis().subsystems().shields().isBroken()) {
            return;
        }

        var travel = self.tardis().travel();
        var globalPos = travel.position();
        World world = globalPos.getWorld();
        BlockPos exteriorPos = globalPos.getPos();

        world.getOtherEntities(null, new Box(exteriorPos).expand(8.0))
                .stream()
                .filter(entity -> entity instanceof ServerPlayerEntity)
                .map(entity -> (ServerPlayerEntity) entity)
                .forEach(player -> {
                    Loyalty loyalty = self.tardis().loyalty().get(player);

                    if (!loyalty.isOf(Loyalty.Type.COMPANION)) {
                        return;
                    }

                    // 根据等级决定生命恢复等级
                    int regenAmplifier = 0; // COMPANION: I
                    if (loyalty.isOf(Loyalty.Type.OWNER)) {
                        regenAmplifier = 2; // III
                    } else if (loyalty.isOf(Loyalty.Type.PILOT)) {
                        regenAmplifier = 1; // II
                    }

                    player.addStatusEffect(
                            new StatusEffectInstance(StatusEffects.REGENERATION, 20, regenAmplifier, true, false, false)
                    );

                    // 水下处理（所有 COMPANION 及以上）
                    if (player.isSubmergedInWater()) {
                        player.addStatusEffect(
                                new StatusEffectInstance(StatusEffects.WATER_BREATHING, 20, 0, true, false, false)
                        );
                        player.setAir(player.getMaxAir());
                    }

                    // OWNER 专属：伤害吸收 I
                    if (loyalty.isOf(Loyalty.Type.OWNER)) {
                        player.addStatusEffect(
                                new StatusEffectInstance(StatusEffects.ABSORPTION, 20, 0, true, false, false)
                        );
                    }
                });
    }
}