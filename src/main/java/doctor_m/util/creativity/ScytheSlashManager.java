package doctor_m.util.creativity;

import doctor_m.config.ConfigManager;
import doctor_m.module.creativity.creativity_data.TlipocaScytheItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScytheSlashManager {

    // 冷却结束时间（tick）
    private static final ConcurrentHashMap<UUID, Long> COOLDOWN_ENDS = new ConcurrentHashMap<>();

    // 层数冷却表（ticks）：1层5秒，2层10秒，3层20秒，4层40秒，5层60秒
    private static final long[] LEVEL_COOLDOWNS = {0, 100, 200, 400, 800, 1200};

    // ========== 服务端：多层蓄力斩击 ==========

    public static void performChargedSlash(ServerWorld world, ServerPlayerEntity player,
                                           ItemStack stack, int level) {
        if (level <= 0 || level > ScytheChargingManager.MAX_CHARGE_LEVEL) return;

        // 冷却检查
        long now = world.getTime();
        Long end = COOLDOWN_ENDS.get(player.getUuid());
        if (end != null && now < end) {
            int sec = (int) Math.ceil((end - now) / 20.0);
            player.sendMessage(Text.translatable("message.doctor_m.scythe.cooldown", sec)
                    .formatted(net.minecraft.util.Formatting.RED), true);
            return;
        }

        var config = ConfigManager.getConfig();

        // 范围：每层+6，最高30格半径
        double reach = level * 6.0;
        float damageMultiplier = 1.0f + level * 0.8f; // 1.8x ~ 5.0x
        float damage = config.slashDamage * damageMultiplier;
        double knockbackStrength = 0.6 + level * 0.5;

        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d slashCenter = eyePos.add(look.multiply(reach * 0.5));

        Box box = new Box(eyePos, eyePos).expand(reach);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        int hitCount = 0;

        for (LivingEntity target : targets) {
            double distSq = target.squaredDistanceTo(eyePos);
            if (distSq > reach * reach) continue;

            Vec3d toTarget = target.getPos().subtract(eyePos).normalize();
            double angle = Math.acos(look.dotProduct(toTarget));
            if (angle > Math.PI / 1.5) continue; // 前方120度

            target.damage(world.getDamageSources().playerAttack(player), damage);

            Vec3d kb = look.multiply(knockbackStrength).add(0, 0.4 + level * 0.15, 0);
            target.setVelocity(target.getVelocity().add(kb));
            target.velocityDirty = true;

            hitCount++;
        }

        // 大范围黑红粒子爆发
        spawnSlashBurstParticles(world, slashCenter, look, reach, level);

        // 威慑音效：凋灵生成 + 凋灵死亡
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.5f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1.2f, 0.6f);

        // 玩家突进
        Vec3d dash = look.multiply(0.5 + level * 0.4);
        player.setVelocity(player.getVelocity().add(dash.x, 0.1, dash.z));
        player.velocityDirty = true;

        // 设置冷却
        long cooldown = LEVEL_COOLDOWNS[level];
        COOLDOWN_ENDS.put(player.getUuid(), now + cooldown);
        player.getItemCooldownManager().set(TlipocaScytheItem.getInstance(), (int) cooldown);

        if (hitCount > 0) {
            player.sendMessage(Text.translatable("message.doctor_m.scythe.charged_hit", level, hitCount)
                    .formatted(net.minecraft.util.Formatting.DARK_RED), true);
        } else {
            player.sendMessage(Text.translatable("message.doctor_m.scythe.charged_miss", level)
                    .formatted(net.minecraft.util.Formatting.GRAY), true);
        }
    }

    // ========== 客户端：蓄力粒子（黑红主题） ==========

    public static void spawnChargeParticlesClient(PlayerEntity player, int useTicks) {
        if (!player.getWorld().isClient) return;
        World world = player.getWorld();
        Vec3d base = player.getPos().add(0, player.getHeight() * 0.8, 0);
        int level = Math.min(useTicks / ScytheChargingManager.TICKS_PER_LEVEL,
                ScytheChargingManager.MAX_CHARGE_LEVEL);

        int count = 3 + level * 2;
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double r = 0.5 + world.random.nextDouble() * (0.6 + level * 0.25);
            double x = base.x + Math.cos(angle) * r;
            double z = base.z + Math.sin(angle) * r;
            double y = base.y + world.random.nextDouble() * 0.2;

            if (world.random.nextBoolean()) {
                world.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.008, 0);
            } else {
                world.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.015, 0);
            }
        }

        // 大团黑烟
        if (world.random.nextFloat() < 0.15f + level * 0.08f) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double r = 0.3 + world.random.nextDouble() * 0.5;
            world.addParticle(ParticleTypes.LARGE_SMOKE,
                    base.x + Math.cos(angle) * r,
                    base.y + 0.1,
                    base.z + Math.sin(angle) * r,
                    0, 0.02, 0);
        }
    }

    public static void spawnLevelUpParticlesClient(PlayerEntity player, int level) {
        if (!player.getWorld().isClient) return;
        World world = player.getWorld();
        Vec3d base = player.getPos().add(0, player.getHeight() * 0.5, 0);

        // 环形火焰爆发
        int particles = 12 + level * 4;
        for (int i = 0; i < particles; i++) {
            double angle = (Math.PI * 2 / particles) * i;
            double r = 1.0 + level * 0.3;
            double x = base.x + Math.cos(angle) * r;
            double z = base.z + Math.sin(angle) * r;
            world.addParticle(ParticleTypes.FLAME, x, base.y, z,
                    Math.cos(angle) * 0.05, 0.05, Math.sin(angle) * 0.05);
        }

        player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.6f - level * 0.12f);
    }

    // ========== 斩击爆发粒子（大范围扇形黑红） ==========

    private static void spawnSlashBurstParticles(ServerWorld world, Vec3d center,
                                                 Vec3d look, double reach, int level) {
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = look.crossProduct(up).normalize();
        if (right.lengthSquared() < 0.001) right = new Vec3d(1, 0, 0);
        Vec3d realUp = right.crossProduct(look).normalize();

        int count = 50 + level * 35;
        double maxAngle = Math.PI / 1.5;
        double minDist = reach * 0.2;
        double maxDist = reach * 0.95;

        for (int i = 0; i < count; i++) {
            double angle = (world.random.nextDouble() - 0.5) * 2 * maxAngle;
            double distance = minDist + world.random.nextDouble() * (maxDist - minDist);
            double hOffset = Math.sin(angle) * distance;
            double fOffset = Math.cos(angle) * distance;

            Vec3d pos = center.add(look.multiply(fOffset))
                    .add(right.multiply(hOffset))
                    .add(realUp.multiply((world.random.nextDouble() - 0.5) * (0.4 + level * 0.2)));

            double spread = 0.12 + level * 0.06;
            pos = pos.add(
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread
            );

            int type = world.random.nextInt(4);
            switch (type) {
                case 0 -> world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0.01);
                case 1 -> world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0.02);
                case 2 -> world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0.01);
                default -> world.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0.01);
            }
        }

        // 地面环形冲击波
        int ringCount = 24 + level * 12;
        for (int i = 0; i < ringCount; i++) {
            double angle = (Math.PI * 2 / ringCount) * i + world.random.nextDouble() * 0.2;
            double r = reach * (0.2 + world.random.nextDouble() * 0.8);
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;
            double y = center.y - 0.5 + world.random.nextDouble() * 0.3;

            world.spawnParticles(ParticleTypes.SMOKE, x, y, z,
                    1, 0, 0.04, 0, 0.015);
        }
    }

    // 兼容旧方法
    public static void performSlash(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
        performChargedSlash(world, player, stack, 1);
    }

    public static void spawnParticlesClient(PlayerEntity player) {}
}