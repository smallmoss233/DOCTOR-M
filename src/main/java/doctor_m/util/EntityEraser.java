package doctor_m.util;

import doctor_m.DOCTORM;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class EntityEraser {

    private static final Vector3f GOLD_COLOR = new Vector3f(1.0f, 0.84f, 0.0f);
    private static final float BEAM_PARTICLE_SIZE = 1.0f;
    private static final float ERASE_PARTICLE_SIZE = 1.5f;

    public static void eraseByRaycast(PlayerEntity shooter, World world, double range) {
        Vec3d start = shooter.getEyePos();
        Vec3d direction = shooter.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(range));

        // 1. 金色弹道
        spawnBeamParticles(world, start, end, range);

        // 2. 射击音效（无论是否命中）
        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                DOCTORM.DE_MAT_GUN_FIRE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // 3. 射线检测
        EntityHitResult hitResult = ProjectileUtil.raycast(
                shooter,
                start,
                end,
                shooter.getBoundingBox().expand(range),
                (entity) -> entity != shooter && !entity.isSpectator() && entity.isAlive(),
                range * range
        );

        if (hitResult != null) {
            Entity target = hitResult.getEntity();
            Vec3d hitPos = hitResult.getPos();

            // 4. 命中爆发粒子
            spawnEraseParticles(world, hitPos);

            // 5. 抹除音效
            world.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    DOCTORM.DE_MAT_GUN_ERASE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // 6. 抹除逻辑
            if (target instanceof PlayerEntity player) {
                erasePlayer(player);
            } else if (target instanceof EnderDragonEntity dragon) {
                dragon.kill();
            } else {
                target.discard();
            }
        } else {
            // 未命中反馈
            world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.5f, 1.5f);
        }
    }

    // ========== 金色弹道粒子 ==========
    private static void spawnBeamParticles(World world, Vec3d start, Vec3d end, double range) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        Vec3d direction = end.subtract(start).normalize();
        double step = 0.35;

        for (double d = 0; d < range; d += step) {
            Vec3d pos = start.add(direction.multiply(d));
            double ox = (world.random.nextDouble() - 0.5) * 0.12;
            double oy = (world.random.nextDouble() - 0.5) * 0.12;
            double oz = (world.random.nextDouble() - 0.5) * 0.12;

            serverWorld.spawnParticles(
                    new DustParticleEffect(GOLD_COLOR, BEAM_PARTICLE_SIZE),
                    pos.x + ox, pos.y + oy, pos.z + oz,
                    1, 0, 0, 0, 0
            );
        }
    }

    // ========== 抹除爆发粒子 ==========
    private static void spawnEraseParticles(World world, Vec3d pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        for (int i = 0; i < 80; i++) {
            double ox = (world.random.nextDouble() - 0.5) * 2.5;
            double oy = (world.random.nextDouble() - 0.5) * 2.5;
            double oz = (world.random.nextDouble() - 0.5) * 2.5;
            double speed = 0.3 + world.random.nextDouble() * 0.5;

            serverWorld.spawnParticles(
                    new DustParticleEffect(GOLD_COLOR, ERASE_PARTICLE_SIZE),
                    pos.x, pos.y, pos.z,
                    1, ox, oy, oz, speed
            );
        }

        serverWorld.spawnParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                pos.x, pos.y, pos.z,
                30, 1.5, 1.5, 1.5, 0.2
        );

        serverWorld.spawnParticles(
                ParticleTypes.FLASH,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0
        );
    }

    public static void erasePlayer(PlayerEntity player) {
        player.getInventory().clear();
        player.getEnderChestInventory().clear();

        player.addExperienceLevels(-player.experienceLevel);
        player.experienceProgress = 0.0f;

        player.kill();

        player.sendMessage(
                net.minecraft.text.Text.translatable("message.doctor_m.de_mat_gun.erased")
                        .formatted(net.minecraft.util.Formatting.RED),
                false
        );
    }
}