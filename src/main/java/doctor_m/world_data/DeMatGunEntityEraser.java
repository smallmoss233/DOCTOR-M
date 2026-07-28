package doctor_m.world_data;

import doctor_m.DOCTORM;
import doctor_m.compat.TimelordRegenCompat;
import doctor_m.Item.data_itme.TimeKeyItem;
import doctor_m.Item.data_itme.TimeKyeFragment.EternalCrystalItem;
import doctor_m.Item.data_itme.TimeKyeFragment.PocketWatchItem;
import doctor_m.Item.data_itme.TimeKyeFragment.RelicGemItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerRecipeBook;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class DeMatGunEntityEraser {

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
            if (target instanceof ItemEntity itemEntity) {
                // 如果掉落物是受保护物品，则跳过删除
                if (isProtectedItem(itemEntity.getStack().getItem())) {
                    return;
                }
                target.discard();
            } else if (target instanceof PlayerEntity player) {
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
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) return;

        boolean hasTimeKey      = hasItem(serverPlayer, TimeKeyItem.class);
        boolean hasEternalCrystal = hasItem(serverPlayer, EternalCrystalItem.class);
        boolean hasPocketWatch  = hasItem(serverPlayer, PocketWatchItem.class);
        boolean hasRelicGem     = hasItem(serverPlayer, RelicGemItem.class);

        boolean protectInventory = hasTimeKey || hasEternalCrystal || hasPocketWatch || hasRelicGem;
        boolean protectDiscard   = hasTimeKey || hasPocketWatch || hasRelicGem;

        // ---- 1. 时间领主兼容 ----
        if (TimelordRegenCompat.isLoaded()) {
            if (TimelordRegenCompat.isTimelord(serverPlayer)) {
                TimelordRegenCompat.RegenInfo info = TimelordRegenCompat.getRegenInfo(serverPlayer);
                if (info != null) info.setUsesLeft(0);
            }
        }

        // ---- 2. 清除状态与物品 ----
        serverPlayer.clearStatusEffects();
        serverPlayer.addExperienceLevels(-serverPlayer.experienceLevel);
        serverPlayer.experienceProgress = 0.0f;

        if (!protectInventory) {
            serverPlayer.getInventory().clear();
            serverPlayer.getEnderChestInventory().clear();
        }

        // ---- 3. 反射清空进度（成就） ----
        try {
            Object tracker = serverPlayer.getAdvancementTracker();
            Field progressField = tracker.getClass().getDeclaredField("advancementToProgress");
            progressField.setAccessible(true);
            Map<?, ?> progressMap = (Map<?, ?>) progressField.get(tracker);
            progressMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---- 4. 反射清空配方 ----
        try {
            ServerRecipeBook recipeBook = serverPlayer.getRecipeBook();
            Field recipesField = ServerRecipeBook.class.getDeclaredField("recipes");
            recipesField.setAccessible(true);
            Set<?> recipesSet = (Set<?>) recipesField.get(recipeBook);
            recipesSet.clear();

            Field displayedField = ServerRecipeBook.class.getDeclaredField("displayedRecipes");
            displayedField.setAccessible(true);
            Set<?> displayedSet = (Set<?>) displayedField.get(recipeBook);
            displayedSet.clear();

            Method sendUnlock = ServerRecipeBook.class.getDeclaredMethod("sendUnlockRecipes", ServerPlayerEntity.class);
            sendUnlock.setAccessible(true);
            sendUnlock.invoke(recipeBook, serverPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---- 5. 反射清空统计 ----
        try {
            StatHandler stats = serverPlayer.getStatHandler();
            Field statMapField = StatHandler.class.getDeclaredField("statMap");
            statMapField.setAccessible(true);
            Map<?, ?> statMap = (Map<?, ?>) statMapField.get(stats);
            statMap.clear();

            Method sendStats = StatHandler.class.getDeclaredMethod("sendStats", ServerPlayerEntity.class);
            sendStats.setAccessible(true);
            sendStats.invoke(stats, serverPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---- 6. 强制保存玩家数据到磁盘（防止回档） ----
        try {
            PlayerManager playerManager = server.getPlayerManager();
            Method saveMethod = PlayerManager.class.getDeclaredMethod("savePlayerData", ServerPlayerEntity.class);
            saveMethod.setAccessible(true);
            saveMethod.invoke(playerManager, serverPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ---- 7. 清除坐骑/乘客 ----
        if (serverPlayer.getVehicle() != null) {
            serverPlayer.getVehicle().discard();
        }
        serverPlayer.dismountVehicle();
        serverPlayer.removeAllPassengers();

        // ---- 8. 物理删除实体（跳过死亡流程） ----
        player.kill();
        if (!protectDiscard) {
            serverPlayer.discard();
        }
    }

    // 检查玩家背包/末影箱中是否含有指定物品
    private static boolean hasItem(ServerPlayerEntity player, Class<?> itemClass) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (itemClass.isInstance(player.getInventory().getStack(i).getItem())) {
                return true;
            }
        }
        for (int i = 0; i < player.getEnderChestInventory().size(); i++) {
            if (itemClass.isInstance(player.getEnderChestInventory().getStack(i).getItem())) {
                return true;
            }
        }
        return false;
    }

    // 新增：判断物品是否属于受保护类型（掉落物防护用）
    private static boolean isProtectedItem(Item item) {
        return item instanceof TimeKeyItem
                || item instanceof EternalCrystalItem
                || item instanceof PocketWatchItem
                || item instanceof RelicGemItem;
    }
}