package doctor_m.mixin.aitmixin;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.SelfDestructHandler;
import dev.amble.ait.core.tardis.manager.ServerTardisManager;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import doctor_m.config.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SelfDestructHandler.class)
public class SelfDestructHandlerMixin {

    private static final List<DelayedTask> TASK_QUEUE = new ArrayList<>();
    private static final AtomicBoolean TICK_LISTENER_REGISTERED = new AtomicBoolean(false);

    private static Field TARDIS_FIELD_CACHE = null;

    private Tardis doctor_m$getTardis(SelfDestructHandler self) {

        if (TARDIS_FIELD_CACHE == null) {
            Class<?> clazz = self.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    TARDIS_FIELD_CACHE = clazz.getDeclaredField("tardis");
                    TARDIS_FIELD_CACHE.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                } catch (Exception e) {
                    AITMod.LOGGER.error("Doctor_M: Failed to locate tardis field via reflection", e);
                    break;
                }
            }
        }

        if (TARDIS_FIELD_CACHE == null) {
            return null;
        }

        try {
            return (Tardis) TARDIS_FIELD_CACHE.get(self);
        } catch (IllegalAccessException e) {
            AITMod.LOGGER.error("Doctor_M: Failed to access tardis field", e);
            return null;
        }
    }

    @Inject(method = "complete", at = @At("HEAD"), cancellable = true)
    private void doctor_m$gradualAnnihilation(CallbackInfo ci) {
        boolean enhancementEnabled;
        try {
            var config = ConfigManager.getConfig();
            if (config == null) {
                AITMod.LOGGER.warn("Doctor_M: Config is null, falling back to vanilla self-destruct");
                return;
            }
            enhancementEnabled = config.enableSelfDestructEnhancement;
        } catch (Exception e) {
            AITMod.LOGGER.error("Doctor_M: Failed to read self-destruct config, using vanilla behavior", e);
            return;
        }

        if (!enhancementEnabled) {
            AITMod.LOGGER.debug("Doctor_M: Self-destruct enhancement disabled by config");
            return;
        }

        ci.cancel();

        int MAX_RADIUS = ConfigManager.getConfig().selfDestructMaxRadius;
        int EXPLOSION_STEPS = ConfigManager.getConfig().selfDestructExplosionSteps;
        int DELAY_PER_STEP = ConfigManager.getConfig().selfDestructDelayPerStep;
        int FINAL_CLEAR_RADIUS = ConfigManager.getConfig().selfDestructFinalClearRadius;
        int KNOCKBACK_RADIUS = ConfigManager.getConfig().selfDestructKnockbackRadius;
        double KNOCKBACK_FORCE = ConfigManager.getConfig().selfDestructKnockbackForce;

        if (MAX_RADIUS > 50 || FINAL_CLEAR_RADIUS > 80) {
            AITMod.LOGGER.warn("Doctor_M: Self-destruct radius is extremely large (max={}, final={}), this may cause long annihilation sequences",
                    MAX_RADIUS, FINAL_CLEAR_RADIUS);
        }

        SelfDestructHandler self = (SelfDestructHandler) (Object) this;
        Tardis tardis = this.doctor_m$getTardis(self);

        if (tardis == null) {
            AITMod.LOGGER.error("Doctor_M: Failed to get tardis instance, aborting annihilation");
            return;
        }

        CachedDirectedGlobalPos exterior = tardis.travel().position();
        ServerWorld world = exterior.getWorld();

        if (world == null || world.isClient()) {
            AITMod.LOGGER.error("Doctor_M: Invalid world for annihilation");
            return;
        }

        BlockPos centerPos = exterior.getPos();
        Vec3d center = Vec3d.ofCenter(centerPos);
        MinecraftServer server = world.getServer();

        AITMod.LOGGER.warn("Doctor_M: Tardis {} has initiated GRADUAL ANNIHILATION sequence! (maxRadius={}, steps={}, finalRadius={})",
                tardis.getUuid(), MAX_RADIUS, EXPLOSION_STEPS, FINAL_CLEAR_RADIUS);

        try {
            ServerTardisManager.getInstance().remove(server, tardis.asServer());
        } catch (Exception e) {
            AITMod.LOGGER.error("Doctor_M: Failed to remove tardis from manager", e);
        }

        if (TICK_LISTENER_REGISTERED.compareAndSet(false, true)) {
            ServerTickEvents.END_SERVER_TICK.register(serverInstance -> {
                synchronized (TASK_QUEUE) {
                    Iterator<DelayedTask> it = TASK_QUEUE.iterator();
                    while (it.hasNext()) {
                        DelayedTask task = it.next();
                        task.ticksRemaining--;

                        if (task.ticksRemaining <= 0) {
                            // 执行前检查 world 是否还存活，防止世界卸载后 NPE/内存泄漏
                            if (task.worldRef != null && (task.worldRef.isClient() || task.worldRef.getServer() == null)) {
                                it.remove();
                                continue;
                            }
                            try {
                                task.runnable.run();
                            } catch (Exception e) {
                                AITMod.LOGGER.error("Doctor_M: Error executing delayed annihilation task", e);
                            }
                            it.remove();
                        }
                    }
                }
            });
        }

        // ==================== 阶段 0：核心坍缩 ====================
        scheduleTask(world, 0, () -> {
            annihilateSphereDirect(world, centerPos, 2);
            applyScreenShake(world, center, 10);
            spawnCollapseEffect(world, center);
        });

        // ==================== 阶段 1~N：逐步扩散 ====================
        for (int step = 1; step <= EXPLOSION_STEPS; step++) {
            final int currentStep = step;
            final double progress = currentStep / (double) EXPLOSION_STEPS;
            final double easedProgress = 1 - Math.pow(1 - progress, 3);
            final int radius = (int) (2 + (MAX_RADIUS - 2) * easedProgress);
            final long baseDelay = 10L + (long) step * DELAY_PER_STEP;

            // 球体清除（自动分帧，大半径不会卡死）
            long afterClear = scheduleAnnihilation(world, centerPos, radius, baseDelay);

            // 效果在清除完成后触发
            scheduleTask(world, afterClear, () -> {
                applyScreenShake(world, center, radius + 20);
                applyKnockback(world, center, radius, KNOCKBACK_RADIUS, KNOCKBACK_FORCE);
                spawnExpansionEffect(world, center, radius, currentStep, EXPLOSION_STEPS);
            });
        }

        // ==================== 终极阶段 ====================
        int finalDelay = 10 + (EXPLOSION_STEPS + 3) * DELAY_PER_STEP;
        long afterUltimate = scheduleAnnihilation(world, centerPos, FINAL_CLEAR_RADIUS, finalDelay);

        scheduleTask(world, afterUltimate + 5, () -> {
            executeTotalObliteration(world, center, centerPos, FINAL_CLEAR_RADIUS);
            spawnGrandFinale(world, center, FINAL_CLEAR_RADIUS);
        });
    }

    // ==================== 分帧调度工具 ====================

    /**
     * 安排一个延迟任务，并绑定 World 引用用于存活检查
     */
    private void scheduleTask(ServerWorld world, long delayTicks, Runnable task) {
        synchronized (TASK_QUEUE) {
            TASK_QUEUE.add(new DelayedTask(delayTicks, task, world));
        }
    }

    /**
     * 安排球体清除，自动根据半径决定是否分帧。
     * 返回"清除完成后的下一个可用 tick 延迟"（相对于当前时间线）
     */
    private long scheduleAnnihilation(ServerWorld world, BlockPos center, int radius, long startDelay) {
        // 小半径（<=10）直接一个 tick 清掉，保持视觉效果
        if (radius <= 10) {
            scheduleTask(world, startDelay, () -> annihilateSphereDirect(world, center, radius));
            return startDelay + 1;
        }

        // 大半径分帧：每 tick 处理 5 层 Y，避免单 tick 卡顿
        int layersPerTick = 5;
        int minY = center.getY() - radius;
        int maxY = center.getY() + radius;
        long currentDelay = startDelay;

        for (int y = minY; y <= maxY; y += layersPerTick) {
            final int layerStart = y;
            final int layerEnd = Math.min(y + layersPerTick - 1, maxY);
            final long taskDelay = currentDelay++;

            scheduleTask(world, taskDelay, () -> {
                annihilateSphereLayers(world, center, radius, layerStart, layerEnd);
            });
        }

        // 非实体清除（掉落物、箭等）放在几何清除之后
        final long entityClearDelay = currentDelay;
        scheduleTask(world, entityClearDelay, () -> {
            int cx = center.getX();
            int cy = center.getY();
            int cz = center.getZ();
            Box box = new Box(
                    cx - radius, cy - radius, cz - radius,
                    cx + radius, cy + radius, cz + radius
            );
            List<Entity> entities = world.getEntitiesByClass(Entity.class, box,
                    e -> !(e instanceof LivingEntity) && !(e instanceof PlayerEntity));
            for (Entity entity : entities) {
                entity.discard();
            }
        });

        return entityClearDelay + 1;
    }

    // ==================== 核心清除逻辑 ====================

    /**
     * 直接清除（用于小半径，单 tick 内完成）
     */
    private void annihilateSphereDirect(ServerWorld world, BlockPos center, int radius) {
        annihilateSphereLayers(world, center, radius, center.getY() - radius, center.getY() + radius);
    }

    /**
     * 清除指定 Y 范围内的球体切片。这是性能关键路径。
     * 增加了 Chunk 预检查，避免强制加载未加载区块。
     */
    private void annihilateSphereLayers(ServerWorld world, BlockPos center, int radius, int yStart, int yEnd) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int radiusSq = radius * radius;

        // 限制 Y 到世界边界
        yStart = Math.max(world.getBottomY(), yStart);
        yEnd = Math.min(world.getTopY() - 1, yEnd);

        for (int y = yStart; y <= yEnd; y++) {
            int yOffset = y - cy;
            int yOffsetSq = yOffset * yOffset;

            for (int x = -radius; x <= radius; x++) {
                int xOffsetSq = x * x;
                // 提前剪枝：如果 x² + y² 已经 > r²，整个 z 循环都不需要
                if (xOffsetSq + yOffsetSq > radiusSq) continue;

                for (int z = -radius; z <= radius; z++) {
                    if (xOffsetSq + yOffsetSq + z * z > radiusSq) continue;

                    BlockPos pos = new BlockPos(cx + x, y, cz + z);

                    // ===== 关键优化：跳过未加载的 Chunk，避免强制加载 =====
                    if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                        continue;
                    }

                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.BARRIER)) {
                        continue;
                    }

                    BlockEntity be = world.getBlockEntity(pos);
                    if (be != null) {
                        world.removeBlockEntity(pos);
                    }

                    // 2 = NOTIFY_LISTENERS, 16 = SKIP_DROPS
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2 | 16);
                }
            }
        }
    }

    // ==================== 效果与辅助方法 ====================

    private void applyScreenShake(ServerWorld world, Vec3d center, int effectRadius) {
        Box box = new Box(center, center).expand(effectRadius);
        List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> true);

        for (ServerPlayerEntity player : players) {
            double distanceSq = player.squaredDistanceTo(center.x, center.y, center.z);
            int duration = (int) (40 + (effectRadius * effectRadius - distanceSq) * 0.2);
            duration = Math.max(20, Math.min(duration, 160));

            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA, duration, 0, false, false, false
            ));
        }
    }

    private void applyKnockback(ServerWorld world, Vec3d center, double currentRadius,
                                int knockbackRadius, double knockbackForce) {
        double innerRadius = currentRadius;
        double outerRadius = currentRadius + knockbackRadius;

        Box box = new Box(center, center).expand(outerRadius);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive);

        for (LivingEntity target : targets) {
            double distance = Math.sqrt(target.squaredDistanceTo(center.x, center.y, center.z));

            if (distance < innerRadius || distance > outerRadius) {
                continue;
            }

            Vec3d targetPos = target.getPos();
            Vec3d direction = targetPos.subtract(center).normalize();

            double proximityFactor = 1 - (distance - innerRadius) / knockbackRadius;
            double force = knockbackForce * (1 + proximityFactor * 2);

            target.setVelocity(
                    direction.x * force,
                    0.8 + proximityFactor * 0.5,
                    direction.z * force
            );
            target.velocityModified = true;
        }
    }

    private void executeTotalObliteration(ServerWorld world, Vec3d center, BlockPos centerPos, int finalRadius) {
        Box killBox = new Box(center, center).expand(finalRadius);
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class, killBox,
                LivingEntity::isAlive
        );

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayerEntity player) {
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
            }
            target.kill();
        }

        world.playSound(null, centerPos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 30f, 0.2f);
        world.playSound(null, centerPos, AITSounds.GROAN, SoundCategory.BLOCKS, 20f, 0.3f);
        world.playSound(null, centerPos, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.BLOCKS, 25f, 0.1f);
    }

    private void spawnCollapseEffect(ServerWorld world, Vec3d center) {
        for (int i = 0; i < 100; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist = 3 + Math.random() * 5;
            double px = center.x + Math.cos(angle) * dist;
            double pz = center.z + Math.sin(angle) * dist;
            double py = center.y + (Math.random() - 0.5) * 4;

            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    px, py, pz, 1,
                    (center.x - px) * 0.1, (center.y - py) * 0.1, (center.z - pz) * 0.1,
                    0.5
            );
        }

        world.spawnParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 3, 0.5, 0.5, 0.5, 1);
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.BLOCKS, 5f, 0.5f);
    }

    private void spawnExpansionEffect(ServerWorld world, Vec3d center, double radius, int step, int totalSteps) {
        int points = (int) (radius * 8);
        for (int i = 0; i < points; i++) {
            double theta = (2 * Math.PI * i) / points;
            double phi = Math.acos(2 * ((double) i / points) - 1);

            double px = center.x + radius * Math.sin(phi) * Math.cos(theta);
            double py = center.y + radius * Math.sin(phi) * Math.sin(theta);
            double pz = center.z + radius * Math.cos(phi);

            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    px, py, pz, 1,
                    0, 0, 0, 0.01
            );
        }

        world.spawnParticles(
                ParticleTypes.WHITE_ASH,
                center.x, center.y, center.z,
                (int) (radius * 4), radius * 0.3, radius * 0.3, radius * 0.3, 0.02
        );

        float volume = 2 + (step / (float) totalSteps) * 6;
        float pitch = 1.2f - (step / (float) totalSteps) * 0.8f;

        world.playSound(
                null, center.x, center.y, center.z,
                SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.BLOCKS,
                volume,
                pitch
        );

        if (step % 5 == 0) {
            world.playSound(
                    null, center.x, center.y, center.z,
                    SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                    SoundCategory.BLOCKS,
                    volume * 0.5f,
                    pitch * 0.5f
            );
        }
    }

    /**
     * 终极粒子效果，数量按 finalRadius 缩放，避免硬编码过多
     */
    private void spawnGrandFinale(ServerWorld world, Vec3d center, int finalRadius) {
        int flashCount = Math.min(100, 20 + finalRadius);
        int rodCount = Math.min(800, 100 + finalRadius * 10);
        int ashCount = Math.min(2000, 200 + finalRadius * 20);

        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, Math.min(50, finalRadius / 2), 5, 5, 5, 1);
        world.spawnParticles(ParticleTypes.FLASH, center.x, center.y, center.z, flashCount, 10, 10, 10, 1);
        world.spawnParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, Math.min(200, finalRadius * 2), 20, 20, 20, 1);

        // 烟柱高度和密度也按半径缩放
        int smokeHeight = Math.min(100, 30 + finalRadius);
        for (int y = 0; y < smokeHeight; y++) {
            double spread = Math.min(y * 0.8, finalRadius * 0.6);
            world.spawnParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x, center.y + y, center.z,
                    (int) (spread * 0.5), spread, 3, spread, 0.02
            );
        }

        int sparkBatches = Math.min(30, 5 + finalRadius / 3);
        for (int i = 0; i < sparkBatches; i++) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    center.x + (Math.random() - 0.5) * finalRadius * 1.6,
                    center.y + Math.random() * 60,
                    center.z + (Math.random() - 0.5) * finalRadius * 1.6,
                    50, 10, 10, 10, 1
            );
        }

        Box globalBox = new Box(center, center).expand(200);
        List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, globalBox, p -> true);
        for (ServerPlayerEntity player : players) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA, 100, 1, false, false, false
            ));
        }
    }

    private static class DelayedTask {
        long ticksRemaining;
        final Runnable runnable;
        final ServerWorld worldRef;

        DelayedTask(long ticksRemaining, Runnable runnable, ServerWorld worldRef) {
            this.ticksRemaining = ticksRemaining;
            this.runnable = runnable;
            this.worldRef = worldRef;
        }
    }
}