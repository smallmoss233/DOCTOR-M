package doctor_m.mixin.aitmixin;

import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import dev.amble.ait.core.entities.FallingTardisEntity;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import doctor_m.util.TardisImpactFeedback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingTardisEntity.class)
public class MixinFallingTardisEntity {

    // ==================== Chunk 强制加载 ====================
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

    @Unique
    private boolean aitmixin$landed = false;

    @Inject(method = "stopFalling", at = @At("HEAD"))
    private void aitmixin$onStopFallingStart(boolean antigravs, CallbackInfo ci) {
        this.aitmixin$landed = true;

        Entity self = (Entity) (Object) this;
        World world = self.getWorld();
        if (world.isClient()) return;

        // 落地瞬间：强制停止所有客户端正在播放的 Elytra 呼啸声
        StopSoundS2CPacket stopPacket = new StopSoundS2CPacket(
                new Identifier("minecraft", "item.elytra.flying"),
                SoundCategory.BLOCKS
        );
        for (ServerPlayerEntity player : ((ServerWorld) world).getPlayers()) {
            if (player.squaredDistanceTo(self) < 256.0) {
                player.networkHandler.sendPacket(stopPacket);
            }
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

    // ==================== 下落开始时：保存锁状态并强制关门上锁 ====================
    @Inject(method = "spawnFromBlock", at = @At("HEAD"))
    private static void aitmixin$saveAndLockDoorsOnFall(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!(world.getBlockEntity(pos) instanceof ExteriorBlockEntity exterior))
            return;
        if (exterior.tardis().isEmpty())
            return;

        Tardis tardis = exterior.tardis().get();
        // 保存当前锁状态（locked() 返回 boolean，直接用）
        tardis.door().previouslyLocked().set(tardis.door().locked());
        // 强制关门并上锁，forced=true 防止 interactLock 覆盖 previouslyLocked
        tardis.door().closeDoors();
        tardis.door().interactLock(true, null, true);
    }

    // ==================== 落地后：恢复之前保存的锁状态 ====================
    @Inject(method = "stopFalling", at = @At("TAIL"))
    private void aitmixin$restoreDoorsOnLand(boolean antigravs, CallbackInfo ci) {
        FallingTardisEntity self = (FallingTardisEntity) (Object) this;
        if (!self.isLinked() || self.tardis().isEmpty())
            return;

        Tardis tardis = self.tardis().get();
        // forced=true 直接恢复，不覆盖 previouslyLocked，也不受 DEMAT/MAT 状态限制
        tardis.door().interactLock(tardis.door().previouslyLocked().get(), null, true);
    }

    // ==================== 再入火焰粒子 + 呼啸（高速下坠时）====================
    @Unique
    private double aitmixin$startY = Double.NaN;

    @Unique
    private static final double AITMIXIN$FLAME_THRESHOLD = 120.0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void aitmixin$recordStartY(CallbackInfo ci) {
        if (Double.isNaN(this.aitmixin$startY)) {
            this.aitmixin$startY = ((Entity) (Object) this).getY();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void aitmixin$spawnReentryFlames(CallbackInfo ci) {
        if (this.aitmixin$landed) return;

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

        if (fallDistance > 60.0 && entity.timeFalling % 20 == 0) {
            float howlVolume = (float) Math.min((fallDistance - 60.0) / 60.0, 1.0) * 1.5f;
            sw.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.BLOCKS,
                    howlVolume, 0.7f);
        }
        if (fallDistance > 90.0 && entity.timeFalling % 30 == 0) {
            sw.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.BLOCKS,
                    0.5f, 2.0f);
        }
    }

    // ==================== 横向偏移检测 ====================
    @Unique
    private Vec3d aitmixin$lastPos = Vec3d.ZERO;

    @Unique
    private static final double AITMIXIN$HORIZONTAL_IMPACT_THRESHOLD = 0.12;

    @Inject(method = "tick", at = @At("TAIL"))
    private void aitmixin$detectExternalDeflection(CallbackInfo ci) {
        if (this.aitmixin$landed) return;

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

    // ==================== 落地冲击反馈（粒子 + 音效 + 内部反馈 + 火焰熄灭）====================
    @Unique
    private static final double AITMIXIN$IMPACT_HEIGHT_THRESHOLD = 120.0;

    @Inject(method = "stopFalling", at = @At("TAIL"))
    private void aitmixin$spawnImpactFeedback(boolean antigravs, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        FallingTardisEntity entity = (FallingTardisEntity) (Object) this;

        double fallDistance = this.aitmixin$startY - self.getY();
        float intensity = (float) Math.min(fallDistance / AITMIXIN$IMPACT_HEIGHT_THRESHOLD, 1.0);

        // 内部反馈
        if (entity.isLinked() && !entity.tardis().isEmpty()) {
            ServerTardis tardis = entity.tardis().get().asServer();
            TardisImpactFeedback.apply(tardis, self.getPos(), intensity);
        }

        World world = self.getWorld();
        if (world.isClient()) return;
        ServerWorld sw = (ServerWorld) world;
        Vec3d pos = self.getPos();

        // ===== 落地音效分级 =====
        if (fallDistance >= 60.0) {
            float explodeVol = (float) Math.min(fallDistance / 30.0, 4.0f);
            sw.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,
                    explodeVol, 0.8f);
        } else if (fallDistance >= 30.0) {
            sw.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS,
                    1.5f, 0.6f);
        } else if (fallDistance >= 20.0) {
            sw.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_METAL_HIT, SoundCategory.BLOCKS,
                    0.8f, 0.7f);
        }

        // ===== 火焰熄灭效果（外壳冒火时）=====
        if (fallDistance > 60.0) {
            sw.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS,
                    1.2f, 1.0f);

            for (int i = 1; i <= 8; i++) {
                final int delay = i * 5;
                Scheduler.get().runTaskLater(() -> {
                    sw.spawnParticles(ParticleTypes.FLAME,
                            pos.x, pos.y + 0.3, pos.z,
                            4, 0.4, 0.2, 0.4, 0.03);
                    sw.spawnParticles(ParticleTypes.SMOKE,
                            pos.x, pos.y + 0.5, pos.z,
                            3, 0.3, 0.3, 0.3, 0.02);
                }, TaskStage.END_SERVER_TICK, TimeUnit.TICKS, delay);
            }
        }

        // 外部冲击粒子云（≥20格）
        if (fallDistance < 20.0) return;

        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

        for (int i = 0; i < 25; i++) {
            double ox = (sw.random.nextDouble() - 0.5) * 4.0;
            double oy = sw.random.nextDouble() * 2.5;
            double oz = (sw.random.nextDouble() - 0.5) * 4.0;
            sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, 0.15, 0.25, 0.15, 0.03);
        }

        for (int i = 0; i < 20; i++) {
            double ox = (sw.random.nextDouble() - 0.5) * 3.0;
            double oz = (sw.random.nextDouble() - 0.5) * 3.0;
            sw.spawnParticles(ParticleTypes.CLOUD,
                    pos.x + ox, pos.y + 0.2, pos.z + oz,
                    1, 0.3, 0.15, 0.3, 0.05);
        }
    }
}