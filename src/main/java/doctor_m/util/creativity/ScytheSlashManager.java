package doctor_m.util.creativity;

import doctor_m.config.ConfigManager;
import doctor_m.module.creativity.creativity_data.TlipocaScytheItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 斩击系统 - 严格分离客户端和服务端逻辑
 */
public class ScytheSlashManager {

    // 移除硬编码常量，改为从配置读取
    private static final ConcurrentHashMap<UUID, Long> lastSlashTime = new ConcurrentHashMap<>();

    // ========== 服务端逻辑 ==========

    /**
     * 服务端执行斩击（只有服务端调用）
     */
    public static void performSlash(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
        if (world.isClient) return;

        var config = ConfigManager.getConfig();

        // 总开关
        if (!config.enableSlashSystem) {
            return;
        }

        long cooldownTicks = config.slashCooldownTicks;

        // 检查冷却
        if (isOnCooldown(world, player, cooldownTicks)) {
            player.sendMessage(Text.translatable("message.doctor_m.scythe.cooldown"), true);
            return;
        }

        setCooldown(world, player, cooldownTicks);

        // 从配置读取参数
        float damage = config.slashDamage;
        double reach = config.slashReach;
        double width = config.slashWidth;
        double height = config.slashHeight;

        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);

        Vec3d center = eyePos.add(look.multiply(reach / 2));
        Box slashBox = new Box(
                center.x - width / 2, center.y - height / 2, center.z - width / 2,
                center.x + width / 2, center.y + height / 2, center.z + width / 2
        );

        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                slashBox,
                entity -> entity != player && entity.isAlive()
        );

        int hitCount = 0;
        for (LivingEntity target : targets) {
            Vec3d toTarget = target.getPos().subtract(eyePos).normalize();
            double angle = Math.acos(look.dotProduct(toTarget));
            if (angle < Math.PI / 3) {
                target.damage(world.getDamageSources().playerAttack(player), damage);
                hitCount++;
            }
        }

        if (hitCount > 0) {
            player.sendMessage(Text.translatable("message.doctor_m.scythe.hit", hitCount), true);
        }
    }

    public static boolean isOnCooldown(World world, PlayerEntity player, long cooldownTicks) {
        Long last = lastSlashTime.get(player.getUuid());
        return last != null && world.getTime() - last < cooldownTicks;
    }

    public static void setCooldown(World world, PlayerEntity player, long cooldownTicks) {
        lastSlashTime.put(player.getUuid(), world.getTime());
        player.getItemCooldownManager().set(
                TlipocaScytheItem.getInstance(),
                (int) cooldownTicks
        );
    }

    // ========== 客户端逻辑 ==========

    /**
     * 客户端生成粒子（只有客户端调用！）
     */
    public static void spawnParticlesClient(PlayerEntity player) {
        if (player.getWorld().isClient) {
            doSpawnParticles(player);
        }
    }

    private static void doSpawnParticles(PlayerEntity player) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);

        Vec3d forward = look.normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up).normalize();
        if (right.lengthSquared() < 0.001) {
            right = new Vec3d(1, 0, 0);
        }
        Vec3d realUp = right.crossProduct(forward).normalize();

        int count = 80;
        double minDistance = 2.0;
        double maxDistance = 6.0;
        double maxAngle = Math.PI / 3;

        for (int i = 0; i < count; i++) {
            double angle = (world.random.nextDouble() - 0.5) * 2 * maxAngle;
            double distance = minDistance + world.random.nextDouble() * (maxDistance - minDistance);

            double horizontalOffset = Math.sin(angle) * distance;
            double forwardOffset = Math.cos(angle) * distance;

            Vec3d basePos = eyePos.add(forward.multiply(forwardOffset));
            Vec3d offset = right.multiply(horizontalOffset);

            double verticalSpread = 0.5;
            Vec3d verticalOffset = realUp.multiply((world.random.nextDouble() - 0.5) * verticalSpread);

            Vec3d pos = basePos.add(offset).add(verticalOffset);

            double spread = 0.15;
            pos = pos.add(
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread
            );

            if (i % 2 == 0) {
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
            } else {
                world.addParticle(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }
}