package doctor_m.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityEraser {

    public static void eraseByRaycast(PlayerEntity shooter, World world) {
        double range = 256.0;
        Vec3d start = shooter.getEyePos();
        Vec3d direction = shooter.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(range));

        EntityHitResult hitResult = ProjectileUtil.raycast(
                shooter,
                start,
                end,
                shooter.getBoundingBox().expand(range),
                (entity) -> entity != shooter && !entity.isSpectator() && entity.isAlive(),
                range
        );

        if (hitResult != null) {
            Entity target = hitResult.getEntity();
            if (target instanceof PlayerEntity player) {
                erasePlayer(player);
            } else {
                target.discard();
            }
            world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        } else {
            world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.5F, 1.5F);
        }
    }

    public static void erasePlayer(PlayerEntity player) {
        // 清空背包和末影箱
        player.getInventory().clear();
        player.getEnderChestInventory().clear();

        // 清空经验
        player.addExperienceLevels(-player.experienceLevel);
        player.experienceProgress = 0.0f;

        // 强制死亡
        player.kill();

        // 提示消息
        player.sendMessage(
                net.minecraft.text.Text.translatable("message.doctor_m.de_mat_gun.erased")
                        .formatted(net.minecraft.util.Formatting.RED),
                false
        );
    }
}