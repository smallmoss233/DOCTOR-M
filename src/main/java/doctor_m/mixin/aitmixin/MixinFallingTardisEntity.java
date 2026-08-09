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
import dev.amble.ait.core.tardis.Tardis;

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
    private static final double AITMIXIN$FLAME_THRESHOLD = 80.0;

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

        // 环绕火焰
        for (int i = 0; i < 4; i++) {
            double ox = (sw.random.nextDouble() - 0.5) * 1.6;
            double oy = (sw.random.nextDouble() - 0.5) * 1.6;
            double oz = (sw.random.nextDouble() - 0.5) * 1.6;
            sw.spawnParticles(ParticleTypes.FLAME,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, 0, 0, 0, 0.05);
        }

        // 烟雾尾迹
        if (entity.timeFalling % 5 == 0) {
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x, pos.y + 0.5, pos.z,
                    3, 0.4, 0.4, 0.4, 0.02);
        }

        // 超高速额外岩浆粒子
        if (fallDistance > 60.0) {
            sw.spawnParticles(ParticleTypes.LAVA,
                    pos.x, pos.y, pos.z,
                    1, 0.5, 0.5, 0.5, 0.1);
        }
    }

    // ==================== 落地冲击粒子云（下坠超过 2 秒）====================
    @Unique
    private static final int AITMIXIN$IMPACT_THRESHOLD = 40; // 2 秒 = 40 ticks

    @Inject(method = "stopFalling", at = @At("TAIL"))
    private void aitmixin$spawnImpactCloud(boolean antigravs, CallbackInfo ci) {
        FallingTardisEntity entity = (FallingTardisEntity) (Object) this;
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