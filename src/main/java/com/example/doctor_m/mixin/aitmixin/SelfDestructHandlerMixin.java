package com.example.doctor_m.mixin.aitmixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.AITMod;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.SelfDestructHandler;
import dev.amble.ait.core.tardis.manager.ServerTardisManager;
import dev.amble.ait.core.tardis.util.TardisUtil;
import dev.amble.lib.data.CachedDirectedGlobalPos;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

@Mixin(SelfDestructHandler.class)
public class SelfDestructHandlerMixin {

    private static final int MAX_RADIUS = 50;
    private static final int EXPLOSION_STEPS = 10;
    private static final int DELAY_PER_STEP = 5;
    private static final int FINAL_KILL_RADIUS = 100;
    private static final int FINAL_EXPLOSION_COUNT = 3;
    private static final double MAX_ARC_DISTANCE = 3.5;

    // ==================== 爆炸威力配置区 ====================
    // 初始中心大爆炸威力
    private static final float INITIAL_EXPLOSION_POWER = 50.0f;
    // 球面扩散爆炸威力（单个小火球）
    private static final float SPHERE_SURFACE_POWER = 3f;
    // 经线纬线插值补充爆炸威力
    private static final float SPHERE_GRID_POWER = 2f;
    // 球内随机填充爆炸威力
    private static final float SPHERE_INNER_POWER = 2f;
    // 终极三连爆威力（依次递增）
    private static final float[] FINAL_EXPLOSION_POWERS = {75.0f, 100.0f, 125.0f};
    // =====================================================

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
    private void doctor_m$epicSelfDestruct(CallbackInfo ci) {
        ci.cancel();

        SelfDestructHandler self = (SelfDestructHandler) (Object) this;
        Tardis tardis = this.doctor_m$getTardis(self);

        if (tardis == null) {
            AITMod.LOGGER.error("Failed to get tardis instance, aborting epic self destruct");
            return;
        }

        CachedDirectedGlobalPos exterior = tardis.travel().position();
        ServerWorld world = exterior.getWorld();
        BlockPos centerPos = exterior.getPos();
        Vec3d center = Vec3d.ofCenter(centerPos);
        MinecraftServer server = world.getServer();

        AITMod.LOGGER.warn("Tardis {} has initiated EPIC self destruct sequence!", tardis.getUuid());

        ServerTardisManager.getInstance().remove(server, tardis.asServer());

        // 阶段 0：初始大爆炸
        spawnInitialExplosion(world, centerPos, center);

        // 阶段 1~N：球形扩散
        for (int step = 1; step <= EXPLOSION_STEPS; step++) {
            final int currentStep = step;
            final double radius = (MAX_RADIUS / (double) EXPLOSION_STEPS) * currentStep;
            final long delayTicks = (long) step * DELAY_PER_STEP;

            scheduleTask(server, delayTicks, () -> {
                spawnDenseSphereExplosion(world, center, radius, currentStep);
            });
        }

        // 终极阶段：3次连续超大爆炸
        int finalBaseDelay = (EXPLOSION_STEPS + 2) * DELAY_PER_STEP;
        for (int i = 0; i < FINAL_EXPLOSION_COUNT; i++) {
            final int explosionIndex = i;
            scheduleTask(server, finalBaseDelay + (long) i * 4, () -> {
                float power = FINAL_EXPLOSION_POWERS[explosionIndex];

                world.createExplosion(
                        null, null, TardisUtil.EXPLOSION_BEHAVIOR,
                        center.x, center.y, center.z,
                        power, TardisUtil.doCreateFire(world),
                        World.ExplosionSourceType.MOB
                );

                if (explosionIndex == FINAL_EXPLOSION_COUNT - 1) {
                    executeFinalKill(world, center, centerPos);
                }

                spawnFinalExplosionParticles(world, center, explosionIndex);
            });
        }
    }

    private void spawnDenseSphereExplosion(ServerWorld world, Vec3d center, double radius, int step) {
        Random random = new Random();

        // 策略1：斐波那契球面
        int points = (int) ((2 * Math.PI * radius / MAX_ARC_DISTANCE) * (Math.PI * radius / MAX_ARC_DISTANCE));
        points = Math.max(points, 16);

        double phi = Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < points; i++) {
            double y = 1.0 - (i / (double) (points - 1)) * 2.0;
            double radiusAtY = Math.sqrt(1.0 - y * y);
            double theta = phi * i;

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            double px = center.x + x * radius;
            double py = center.y + y * radius;
            double pz = center.z + z * radius;

            // 使用配置的球面爆炸威力
            world.createExplosion(
                    null, null, TardisUtil.EXPLOSION_BEHAVIOR,
                    px, py, pz,
                    SPHERE_SURFACE_POWER + random.nextFloat() * 0.5f, // 加一点随机变化
                    TardisUtil.doCreateFire(world),
                    World.ExplosionSourceType.MOB
            );

            world.spawnParticles(
                    ParticleTypes.EXPLOSION,
                    px, py, pz, 1,
                    0.2, 0.2, 0.2, 0.05
            );
        }

        // 策略2：经线纬线插值
        int meridians = (int) (Math.PI * radius / MAX_ARC_DISTANCE);
        int parallels = (int) (radius / MAX_ARC_DISTANCE) + 1;

        for (int m = 0; m < meridians; m++) {
            double theta = (2 * Math.PI * m) / meridians;
            for (int p = 1; p < parallels; p++) {
                double y = Math.cos(Math.PI * p / parallels);
                double rCircle = Math.sqrt(1 - y * y);

                double px = center.x + Math.cos(theta) * rCircle * radius;
                double py = center.y + y * radius;
                double pz = center.z + Math.sin(theta) * rCircle * radius;

                if (random.nextDouble() < 0.6) {
                    // 使用配置的网格插值爆炸威力
                    world.createExplosion(
                            null, null, TardisUtil.EXPLOSION_BEHAVIOR,
                            px, py, pz,
                            SPHERE_GRID_POWER,
                            TardisUtil.doCreateFire(world),
                            World.ExplosionSourceType.MOB
                    );
                }
            }
        }

        // 策略3：球内随机填充
        int innerPoints = (int) (radius * 2);
        for (int i = 0; i < innerPoints; i++) {
            double u = random.nextDouble();
            double r = radius * Math.cbrt(u);
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi_rand = Math.acos(2 * random.nextDouble() - 1);

            double px = center.x + r * Math.sin(phi_rand) * Math.cos(theta);
            double py = center.y + r * Math.sin(phi_rand) * Math.sin(theta);
            double pz = center.z + r * Math.cos(phi_rand);

            // 使用配置的球内爆炸威力
            world.createExplosion(
                    null, null, TardisUtil.EXPLOSION_BEHAVIOR,
                    px, py, pz,
                    SPHERE_INNER_POWER,
                    TardisUtil.doCreateFire(world),
                    World.ExplosionSourceType.MOB
            );
        }

        world.spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x, center.y, center.z,
                (int) (radius * 4), radius * 0.25, radius * 0.25, radius * 0.25, 0.08
        );
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                center.x, center.y, center.z,
                (int) (radius * 2.5), radius * 0.15, radius * 0.15, radius * 0.15, 0.04
        );

        world.playSound(
                null, center.x, center.y, center.z,
                SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE,
                SoundCategory.BLOCKS,
                5f,
                0.3f + random.nextFloat() * 0.7f
        );
    }

    private void executeFinalKill(ServerWorld world, Vec3d center, BlockPos centerPos) {
        Box killBox = new Box(center, center).expand(FINAL_KILL_RADIUS);
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class, killBox,
                LivingEntity::isAlive
        );

        DamageSource absolute = world.getDamageSources().genericKill();

        for (LivingEntity target : targets) {
            if (target instanceof PlayerEntity player) {
                player.getInventory().dropAll();
            }
            target.damage(absolute, Float.MAX_VALUE);
            if (target.isAlive()) {
                target.kill();
            }
        }

        spawnUltimateParticles(world, center);

        world.playSound(null, centerPos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 20f, 0.3f);
        world.playSound(null, centerPos, AITSounds.GROAN, SoundCategory.BLOCKS, 15f, 0.5f);
    }

    private void spawnInitialExplosion(ServerWorld world, BlockPos pos, Vec3d center) {
        world.createExplosion(
                null, null, TardisUtil.EXPLOSION_BEHAVIOR,
                center.x, center.y, center.z,
                INITIAL_EXPLOSION_POWER, // 使用配置的初始爆炸威力
                TardisUtil.doCreateFire(world),
                World.ExplosionSourceType.MOB
        );

        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 20, 2, 2, 2, 1);
        world.spawnParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 10, 1, 1, 1, 1);
        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 100, 3, 3, 3, 0.5);
        world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.BLOCKS, 10f, 0.8f);
    }

    private void spawnFinalExplosionParticles(ServerWorld world, Vec3d center, int index) {
        switch (index) {
            case 0 -> {
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 15, 5, 5, 5, 1);
                world.spawnParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 5, 3, 3, 3, 1);
            }
            case 1 -> {
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 25, 8, 8, 8, 1);
                world.spawnParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 100, 10, 5, 10, 0.5);
            }
            case 2 -> {
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 40, 12, 12, 12, 1);
                world.spawnParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 20, 8, 8, 8, 1);
            }
        }
    }

    private void spawnUltimateParticles(ServerWorld world, Vec3d center) {
        for (int y = 0; y < 80; y++) {
            double spread = y * 0.6;
            int count = (int) (spread * 1.5);
            world.spawnParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x, center.y + y, center.z,
                    count, spread, 2, spread, 0.03
            );
        }

        world.spawnParticles(
                ParticleTypes.SONIC_BOOM,
                center.x, center.y, center.z,
                80, 25, 8, 25, 1
        );

        world.spawnParticles(
                ParticleTypes.LAVA,
                center.x, center.y, center.z,
                300, 40, 15, 40, 0.5
        );

        for (int i = 0; i < 15; i++) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    center.x + (Math.random() - 0.5) * 50,
                    center.y + Math.random() * 40,
                    center.z + (Math.random() - 0.5) * 50,
                    30, 8, 8, 8, 1
            );
        }

        world.spawnParticles(
                ParticleTypes.END_ROD,
                center.x, center.y + 20, center.z,
                200, 30, 20, 30, 0.3
        );
    }

    private void scheduleTask(MinecraftServer server, long delayTicks, Runnable task) {
        if (delayTicks <= 0) {
            server.executeSync(task);
            return;
        }

        server.executeSync(new Runnable() {
            private long remaining = delayTicks;

            @Override
            public void run() {
                remaining--;
                if (remaining <= 0) {
                    task.run();
                } else {
                    server.executeSync(this);
                }
            }
        });
    }
}