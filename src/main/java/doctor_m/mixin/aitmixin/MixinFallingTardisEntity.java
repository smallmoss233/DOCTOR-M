package doctor_m.mixin.aitmixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.entities.FallingTardisEntity;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.Tardis;
import doctor_m.util.TardisImpactFeedback;

@Mixin(FallingTardisEntity.class)
public class MixinFallingTardisEntity {

    // ==================== Chunk 强制加载（防止下落途中消失）====================
    @Unique
    private ChunkPos aitmixin$forcedChunkPos;

    @Inject(
            method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/block/BlockState;Ldev/amble/ait/core/tardis/Tardis;)V",
            at = @At("RETURN")
    )
    private void aitmixin$forceChunkOnSpawn(World world, Vec3d pos, BlockState state,
                                            Tardis tardis, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld) {
            this.aitmixin$forcedChunkPos = new ChunkPos(((Entity) (Object) this).getBlockPos());
            serverWorld.getChunkManager().setChunkForced(this.aitmixin$forcedChunkPos, true);
        }
    }

    @Inject(method = "stopFalling", at = @At("TAIL"))
    private void aitmixin$releaseChunkOnLand(boolean antigravs, CallbackInfo ci) {
        if (this.aitmixin$forcedChunkPos != null) {
            World world = ((Entity) (Object) this).getWorld();
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.getChunkManager().setChunkForced(this.aitmixin$forcedChunkPos, false);
            }
            this.aitmixin$forcedChunkPos = null;
        }
    }

    // ==================== ISS 再入火焰粒子（高速下坠时）====================
    @Unique
    private double aitmixin$startY = Double.NaN;

    @Unique
    private static final double AITMIXIN$FLAME_THRESHOLD = 30.0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void aitmixin$recordStartY(CallbackInfo ci) {
        if (Double.isNaN(this.aitmixin$startY)) {
            this.aitmixin$startY = ((Entity) (Object) this).getY();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void aitmixin$spawnReentryFlames(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;

        double fallDistance = this.aitmixin$startY - self.getY();
        if (fallDistance <= AITMIXIN$FLAME_THRESHOLD) return;

        ServerWorld sw = (ServerWorld) world;
        Vec3d pos = self.getPos();
        FallingTardisEntity entity = (FallingTardisEntity) (Object) this;

        for (int i = 0; i < 4; i++) {
            double ox = (sw.random.nextDouble() - 0.5) * 1.6;
            double oy = (sw.random.nextDouble() - 0.5) * 1.6;
            double oz = (sw.random.nextDouble() - 0.5) * 1.6;
            sw.spawnParticles(ParticleTypes.FLAME,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, 0, 0, 0, 0.05);
        }

        if (entity.timeFalling % 5 == 0) {
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x, pos.y + 0.5, pos.z,
                    3, 0.4, 0.4, 0.4, 0.02);
        }

        if (fallDistance > 60.0) {
            sw.spawnParticles(ParticleTypes.LAVA,
                    pos.x, pos.y, pos.z,
                    1, 0.5, 0.5, 0.5, 0.1);
        }
    }

    // ==================== 下落中被外力横向偏移检测 ====================
    @Unique
    private Vec3d aitmixin$lastPos = Vec3d.ZERO;

    @Unique
    private static final double AITMIXIN$HORIZONTAL_IMPACT_THRESHOLD = 0.12;

    @Inject(method = "tick", at = @At("TAIL"))
    private void aitmixin$detectExternalDeflection(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;

        Vec3d currentPos = self.getPos();
        if (aitmixin$lastPos.equals(Vec3d.ZERO)) {
            aitmixin$lastPos = currentPos;
            return;
        }

        Vec3d motion = currentPos.subtract(aitmixin$lastPos);
        aitmixin$lastPos = currentPos;

        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontalSpeed <= AITMIXIN$HORIZONTAL_IMPACT_THRESHOLD) return;

        FallingTardisEntity entity = (FallingTardisEntity) (Object) this;
        if (!entity.isLinked() || entity.tardis().isEmpty()) return;

        ServerTardis tardis = entity.tardis().get().asServer();
        float intensity = (float) Math.min(horizontalSpeed * 3.0, 0.8f);
        TardisImpactFeedback.apply(tardis, self.getPos(), intensity);
    }

    // ==================== 落地冲击反馈（内部 + 外部粒子）====================
    @Unique
    private static final int AITMIXIN$IMPACT_THRESHOLD = 40; // 2 秒

    @Inject(method = "stopFalling", at = @At("TAIL"))
    private void aitmixin$spawnImpactFeedback(boolean antigravs, CallbackInfo ci) {
        FallingTardisEntity entity = (FallingTardisEntity) (Object) this;

        // 计算冲击强度：下坠时间越长，冲击越强
        float intensity = Math.min(entity.timeFalling / 40.0f, 1.0f);

        // 向内部发送运动反馈（声音、粒子、屏幕抖动）
        if (entity.isLinked() && !entity.tardis().isEmpty()) {
            ServerTardis tardis = entity.tardis().get().asServer();
            Vec3d pos = ((Entity) (Object) this).getPos();
            TardisImpactFeedback.apply(tardis, pos, intensity);
        }

        // 下坠超过 2 秒额外在外部世界 spawn 落地冲击粒子云
        if (entity.timeFalling < AITMIXIN$IMPACT_THRESHOLD) return;

        Entity self = (Entity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;

        ServerWorld sw = (ServerWorld) world;
        Vec3d pos = self.getPos();

        // 爆炸核心
        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

        // 被弹开的烟雾云
        for (int i = 0; i < 25; i++) {
            double ox = (sw.random.nextDouble() - 0.5) * 4.0;
            double oy = sw.random.nextDouble() * 2.5;
            double oz = (sw.random.nextDouble() - 0.5) * 4.0;
            sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, 0.15, 0.25, 0.15, 0.03);
        }

        // 尘土飞扬
        for (int i = 0; i < 20; i++) {
            double ox = (sw.random.nextDouble() - 0.5) * 3.0;
            double oz = (sw.random.nextDouble() - 0.5) * 3.0;
            sw.spawnParticles(ParticleTypes.CLOUD,
                    pos.x + ox, pos.y + 0.2, pos.z + oz,
                    1, 0.3, 0.15, 0.3, 0.05);
        }
    }
}