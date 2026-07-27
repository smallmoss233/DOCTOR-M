package doctor_m.mixin.aitmixin;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.SelfDestructHandler;
import dev.amble.ait.core.tardis.manager.ServerTardisManager;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import doctor_m.config.ConfigManager;
import doctor_m.world_data.TimeKey.TimeKeyFunction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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

@Mixin(SelfDestructHandler.class)
public class SelfDestructHandlerMixin {

    // 不再使用硬编码常量，改为从配置读取

    private static final List<DelayedTask> TASK_QUEUE = new ArrayList<>();
    private static boolean TICK_LISTENER_REGISTERED = false;

    private Tardis doctor_m$getTardis(SelfDestructHandler self) {
        try {
            Class<?> clazz = self.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    Field field = clazz.getDeclaredField("tardis");
                    field.setAccessible(true);
                    return (Tardis) field.get(self);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (IllegalAccessException e) {
            AITMod.LOGGER.error("Failed to access tardis field via reflection", e);
        }
        return null;
    }

    @Inject(method = "complete", at = @At("HEAD"), cancellable = true)
    private void doctor_m$gradualAnnihilation(CallbackInfo ci) {
        // 检查总开关
        if (!ConfigManager.getConfig().enableSelfDestructEnhancement) {
            return; // 让原方法执行
        }
        ci.cancel();

        // 读取配置参数
        int MAX_RADIUS = ConfigManager.getConfig().selfDestructMaxRadius;
        int EXPLOSION_STEPS = ConfigManager.getConfig().selfDestructExplosionSteps;
        int DELAY_PER_STEP = ConfigManager.getConfig().selfDestructDelayPerStep;
        int FINAL_CLEAR_RADIUS = ConfigManager.getConfig().selfDestructFinalClearRadius;
        int KNOCKBACK_RADIUS = ConfigManager.getConfig().selfDestructKnockbackRadius;
        double KNOCKBACK_FORCE = ConfigManager.getConfig().selfDestructKnockbackForce;

        SelfDestructHandler self = (SelfDestructHandler) (Object) this;
        Tardis tardis = this.doctor_m$getTardis(self);

        if (tardis == null) {
            AITMod.LOGGER.error("Failed to get tardis instance, aborting annihilation");
            return;
        }

        CachedDirectedGlobalPos exterior = tardis.travel().position();
        ServerWorld world = exterior.getWorld();
        BlockPos centerPos = exterior.getPos();
        Vec3d center = Vec3d.ofCenter(centerPos);
        MinecraftServer server = world.getServer();

        AITMod.LOGGER.warn("Tardis {} has initiated GRADUAL ANNIHILATION sequence!", tardis.getUuid());

        ServerTardisManager.getInstance().remove(server, tardis.asServer());

        if (!TICK_LISTENER_REGISTERED) {
            ServerTickEvents.END_SERVER_TICK.register(serverInstance -> {
                synchronized (TASK_QUEUE) {
                    Iterator<DelayedTask> it = TASK_QUEUE.iterator();
                    while (it.hasNext()) {
                        DelayedTask task = it.next();
                        task.ticksRemaining--;
                        if (task.ticksRemaining <= 0) {
                            try {
                                task.runnable.run();
                            } catch (Exception e) {
                                AITMod.LOGGER.error("Error executing delayed annihilation task", e);
                            }
                            it.remove();
                        }
                    }
                }
            });
            TICK_LISTENER_REGISTERED = true;
        }

        // 阶段 0：核心坍缩
        scheduleTask(0, () -> {
            annihilateSphere(world, centerPos, 2);
            applyScreenShake(world, center, 10);
            spawnCollapseEffect(world, center);
        });

        // 阶段 1~N：逐步扩散
        for (int step = 1; step <= EXPLOSION_STEPS; step++) {
            final int currentStep = step;
            final double progress = currentStep / (double) EXPLOSION_STEPS;
            final double easedProgress = 1 - Math.pow(1 - progress, 3);
            final double radius = 2 + (MAX_RADIUS - 2) * easedProgress;
            final long delayTicks = 10L + (long) step * DELAY_PER_STEP;

            scheduleTask(delayTicks, () -> {
                annihilateSphere(world, centerPos, (int) radius);
                applyScreenShake(world, center, (int) radius + 20);
                applyKnockback(world, center, radius, KNOCKBACK_RADIUS, KNOCKBACK_FORCE);
                spawnExpansionEffect(world, center, radius, currentStep, EXPLOSION_STEPS);
            });
        }

        // 终极阶段
        int finalDelay = 10 + (EXPLOSION_STEPS + 3) * DELAY_PER_STEP;
        scheduleTask(finalDelay, () -> {
            ultimateAnnihilation(world, centerPos, center, FINAL_CLEAR_RADIUS);
            executeTotalObliteration(world, center, centerPos, FINAL_CLEAR_RADIUS);
            spawnGrandFinale(world, center);
        });
    }

    private void scheduleTask(long delayTicks, Runnable task) {
        synchronized (TASK_QUEUE) {
            TASK_QUEUE.add(new DelayedTask(delayTicks, task));
        }
    }

    private void annihilateSphere(ServerWorld world, BlockPos center, int radius) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSq) continue;

                    BlockPos pos = new BlockPos(cx + x, cy + y, cz + z);
                    BlockState state = world.getBlockState(pos);

                    if (state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.BARRIER)) {
                        continue;
                    }

                    BlockEntity be = world.getBlockEntity(pos);
                    if (be != null) {
                        world.removeBlockEntity(pos);
                    }

                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2 | 16);
                }
            }
        }

        Box box = new Box(
                cx - radius, cy - radius, cz - radius,
                cx + radius, cy + radius, cz + radius
        );
        List<Entity> entities = world.getEntitiesByClass(Entity.class, box,
                e -> !(e instanceof LivingEntity) && !(e instanceof PlayerEntity));
        for (Entity entity : entities) {
            entity.discard();
        }
    }

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

    private void ultimateAnnihilation(ServerWorld world, BlockPos center, Vec3d centerVec, int finalRadius) {
        int radius = finalRadius;
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int radiusSq = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSq) continue;

                    BlockPos pos = new BlockPos(cx + x, cy + y, cz + z);
                    BlockState state = world.getBlockState(pos);

                    if (state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.BARRIER)) {
                        continue;
                    }

                    BlockEntity be = world.getBlockEntity(pos);
                    if (be != null) {
                        world.removeBlockEntity(pos);
                    }

                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2 | 16);
                }
            }
        }

        world.spawnParticles(ParticleTypes.FLASH, centerVec.x, centerVec.y, centerVec.z, 200, 60, 60, 60, 1);
        world.spawnParticles(ParticleTypes.END_ROD, centerVec.x, centerVec.y, centerVec.z, 800, 80, 80, 80, 0.5);
        world.spawnParticles(ParticleTypes.WHITE_ASH, centerVec.x, centerVec.y, centerVec.z, 2000, 100, 100, 100, 0.1);
    }

    private void executeTotalObliteration(ServerWorld world, Vec3d center, BlockPos centerPos, int finalRadius) {
        Box killBox = new Box(center, center).expand(finalRadius);
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class, killBox,
                LivingEntity::isAlive
        );

        DamageSource absolute = world.getDamageSources().genericKill();

        for (LivingEntity target : targets) {
            // ★ 新增：跳过所有受时间钥匙保护的玩家
            if (target instanceof ServerPlayerEntity player &&
                    TimeKeyFunction.isTimeKeyEquipped(player)) {
                continue;
            }

            if (target instanceof PlayerEntity player) {
                player.getInventory().dropAll();
            }
            target.damage(absolute, Float.MAX_VALUE);
            if (target.isAlive()) target.kill();
            if (target.isAlive()) target.setHealth(0);
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

    private void spawnGrandFinale(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 50, 5, 5, 5, 1);
        world.spawnParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 100, 10, 10, 10, 1);
        world.spawnParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 200, 20, 20, 20, 1);

        for (int y = 0; y < 100; y++) {
            double spread = Math.min(y * 0.8, 60);
            world.spawnParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x, center.y + y, center.z,
                    (int) (spread * 0.5), spread, 3, spread, 0.02
            );
        }

        for (int i = 0; i < 30; i++) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    center.x + (Math.random() - 0.5) * 80,
                    center.y + Math.random() * 60,
                    center.z + (Math.random() - 0.5) * 80,
                    50, 10, 10, 10, 1
            );
        }

        // 终极大爆炸时给所有附近玩家屏幕震动（不含黑暗）
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

        DelayedTask(long ticksRemaining, Runnable runnable) {
            this.ticksRemaining = ticksRemaining;
            this.runnable = runnable;
        }
    }
}