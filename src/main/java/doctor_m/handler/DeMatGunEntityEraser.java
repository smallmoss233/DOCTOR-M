package doctor_m.handler;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.DOCTORM;
import doctor_m.Item.KeytoTime;
import doctor_m.compat.TimelordRegenCompat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerRecipeBook;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DeMatGunEntityEraser {

    private static final Vector3f GOLD_COLOR = new Vector3f(1.0f, 0.84f, 0.0f);
    private static final float BEAM_PARTICLE_SIZE = 1.0f;
    private static final float ERASE_PARTICLE_SIZE = 1.5f;

    public static void eraseByRaycast(PlayerEntity shooter, World world, double range) {
        Vec3d start = shooter.getEyePos();
        Vec3d direction = shooter.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(range));

        spawnBeamParticles(world, start, end, range);

        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                DOCTORM.DE_MAT_GUN_FIRE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        EntityHitResult hitResult = ProjectileUtil.raycast(
                shooter, start, end,
                shooter.getBoundingBox().expand(range),
                (entity) -> entity != shooter && !entity.isSpectator() && entity.isAlive(),
                range * range
        );

        if (hitResult != null) {
            Entity target = hitResult.getEntity();
            Vec3d hitPos = hitResult.getPos();

            spawnEraseParticles(world, hitPos);
            world.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    DOCTORM.DE_MAT_GUN_ERASE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (target instanceof ItemEntity itemEntity) {
                if (itemEntity.getStack().getItem() instanceof KeytoTime) return;
                target.discard();
            } else if (target instanceof PlayerEntity player) {
                erasePlayer(player);
            } else if (target instanceof EnderDragonEntity dragon) {
                dragon.kill();
            } else {
                target.discard();
            }
        }
    }

    private static void spawnBeamParticles(World world, Vec3d start, Vec3d end, double range) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        Vec3d direction = end.subtract(start).normalize();
        for (double d = 0; d < range; d += 0.35) {
            Vec3d pos = start.add(direction.multiply(d));
            double ox = (world.random.nextDouble() - 0.5) * 0.12;
            double oy = (world.random.nextDouble() - 0.5) * 0.12;
            double oz = (world.random.nextDouble() - 0.5) * 0.12;
            serverWorld.spawnParticles(
                    new DustParticleEffect(GOLD_COLOR, BEAM_PARTICLE_SIZE),
                    pos.x + ox, pos.y + oy, pos.z + oz, 1, 0, 0, 0, 0
            );
        }
    }

    private static void spawnEraseParticles(World world, Vec3d pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        for (int i = 0; i < 80; i++) {
            double ox = (world.random.nextDouble() - 0.5) * 2.5;
            double oy = (world.random.nextDouble() - 0.5) * 2.5;
            double oz = (world.random.nextDouble() - 0.5) * 2.5;
            double speed = 0.3 + world.random.nextDouble() * 0.5;
            serverWorld.spawnParticles(
                    new DustParticleEffect(GOLD_COLOR, ERASE_PARTICLE_SIZE),
                    pos.x, pos.y, pos.z, 1, ox, oy, oz, speed
            );
        }
        serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y, pos.z, 30, 1.5, 1.5, 1.5, 0.2);
        serverWorld.spawnParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    public static void erasePlayer(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) return;

        boolean isProtected = hasProtectedItem(serverPlayer);

        if (!isProtected) {
            erasePlayerFromTimeline(serverPlayer, server);
        } else {
            player.kill();
        }
    }

    /**
     * 从时间线上彻底抹除玩家。不使用常规 API，直接操作底层数据。
     */
    private static void erasePlayerFromTimeline(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        String name = player.getEntityName();

        // ===== 1. 先清空玩家自身所有数据 =====
        player.clearStatusEffects();
        player.addExperienceLevels(-player.experienceLevel);
        player.experienceProgress = 0.0f;
        player.totalExperience = 0;
        player.getInventory().clear();
        player.getEnderChestInventory().clear();
        clearTrinkets(player);

        if (TimelordRegenCompat.isLoaded() && TimelordRegenCompat.isTimelord(player)) {
            TimelordRegenCompat.RegenInfo info = TimelordRegenCompat.getRegenInfo(player);
            if (info != null) info.setUsesLeft(0);
        }

        clearAdvancements(player);
        clearRecipes(player);
        clearStats(player);

        // ===== 2. Scoreboard 彻底抹除 =====
        try {
            Scoreboard scoreboard = server.getScoreboard();
            // 更暴力：反射直接清空该玩家在所有 objective 中的 entry
            Field stateField = Scoreboard.class.getDeclaredField("state");
            stateField.setAccessible(true);
            Object state = stateField.get(scoreboard);
            if (state != null) {
                Field scoresField = state.getClass().getDeclaredField("playerScores");
                scoresField.setAccessible(true);
                Map<?, ?> scores = (Map<?, ?>) scoresField.get(state);
                scores.remove(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 3. 强制保存一次空状态（让当前内存中的空数据写入磁盘）=====
        forceSaveEmptyPlayerData(player, server, uuid);

        // ===== 4. 从 ServerWorld 的实体列表中暴力移除 =====
        try {
            ServerWorld world = player.getServerWorld();

            // 4.1 从 world.players 中移除
            Field playersField = ServerWorld.class.getDeclaredField("players");
            playersField.setAccessible(true);
            List<?> players = (List<?>) playersField.get(world);
            players.remove(player);

            // 4.2 从 entityList 中移除（1.20+ 可能是 entityList 或 entityManager）
            try {
                Field entityListField = ServerWorld.class.getDeclaredField("entityList");
                entityListField.setAccessible(true);
                Object entityList = entityListField.get(world);
                Method removeMethod = entityList.getClass().getDeclaredMethod("remove", Entity.class);
                removeMethod.invoke(entityList, player);
            } catch (Exception ex) {
                // 备用：尝试 entityManager
                try {
                    Field entityManagerField = ServerWorld.class.getDeclaredField("entityManager");
                    entityManagerField.setAccessible(true);
                    Object entityManager = entityManagerField.get(world);
                    Method removeMethod = entityManager.getClass().getDeclaredMethod("remove", Entity.class);
                    removeMethod.invoke(entityManager, player);
                } catch (Exception ex2) {
                    ex2.printStackTrace();
                }
            }

            // 4.3 从 chunk 的 entity 列表中移除
            try {
                Field chunkManagerField = ServerWorld.class.getDeclaredField("chunkManager");
                chunkManagerField.setAccessible(true);
                Object chunkManager = chunkManagerField.get(world);
                Field entityTrackersField = chunkManager.getClass().getDeclaredField("entityTrackers");
                entityTrackersField.setAccessible(true);
                Map<?, ?> trackers = (Map<?, ?>) entityTrackersField.get(chunkManager);
                trackers.remove(player.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 5. 从 PlayerManager 中暴力移除 =====
        try {
            PlayerManager pm = server.getPlayerManager();

            // 5.1 players 列表
            Field playersField = PlayerManager.class.getDeclaredField("players");
            playersField.setAccessible(true);
            List<?> players = (List<?>) playersField.get(pm);
            players.remove(player);

            // 5.2 playerMap (UUID -> Player)
            try {
                Field playerMapField = PlayerManager.class.getDeclaredField("playerMap");
                playerMapField.setAccessible(true);
                Map<?, ?> playerMap = (Map<?, ?>) playerMapField.get(pm);
                playerMap.remove(uuid);
            } catch (Exception e) {
                // 备用字段名
                try {
                    Field playersByUuidField = PlayerManager.class.getDeclaredField("playersByUuid");
                    playersByUuidField.setAccessible(true);
                    Map<?, ?> playersByUuid = (Map<?, ?>) playersByUuidField.get(pm);
                    playersByUuid.remove(uuid);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // 5.3 统计缓存
            try {
                Field statisticsField = PlayerManager.class.getDeclaredField("statisticsMap");
                statisticsField.setAccessible(true);
                Map<?, ?> statisticsMap = (Map<?, ?>) statisticsField.get(pm);
                statisticsMap.remove(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 5.4 进度缓存
            try {
                Field advancementsField = PlayerManager.class.getDeclaredField("advancementTrackers");
                advancementsField.setAccessible(true);
                Map<?, ?> advancementTrackers = (Map<?, ?>) advancementsField.get(pm);
                advancementTrackers.remove(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== 6. 文件系统层面删除玩家数据（最暴力）=====
        deletePlayerFiles(server, uuid);

        // ===== 7. 物理删除实体 =====
        player.kill();
        player.discard();

        // 8. 强制断开网络连接（如果还在线）
        try {
            player.networkHandler.disconnect(net.minecraft.text.Text.literal("ERASED_FROM_TIMELINE"));
        } catch (Exception ignored) {}

        // 9. 提示 GC（可选，心理安慰）
        System.gc();
    }

    // ========== 辅助方法 ==========

    private static void clearAdvancements(ServerPlayerEntity player) {
        try {
            Object tracker = player.getAdvancementTracker();
            Field progressField = tracker.getClass().getDeclaredField("advancementToProgress");
            progressField.setAccessible(true);
            Map<?, ?> progressMap = (Map<?, ?>) progressField.get(tracker);
            progressMap.clear();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void clearRecipes(ServerPlayerEntity player) {
        try {
            ServerRecipeBook recipeBook = player.getRecipeBook();
            Field recipesField = ServerRecipeBook.class.getDeclaredField("recipes");
            recipesField.setAccessible(true);
            ((Set<?>) recipesField.get(recipeBook)).clear();

            Field displayedField = ServerRecipeBook.class.getDeclaredField("displayedRecipes");
            displayedField.setAccessible(true);
            ((Set<?>) displayedField.get(recipeBook)).clear();

            Method sendUnlock = ServerRecipeBook.class.getDeclaredMethod("sendUnlockRecipes", ServerPlayerEntity.class);
            sendUnlock.setAccessible(true);
            sendUnlock.invoke(recipeBook, player);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void clearStats(ServerPlayerEntity player) {
        try {
            StatHandler stats = player.getStatHandler();
            Field statMapField = StatHandler.class.getDeclaredField("statMap");
            statMapField.setAccessible(true);
            ((Map<?, ?>) statMapField.get(stats)).clear();

            Method sendStats = StatHandler.class.getDeclaredMethod("sendStats", ServerPlayerEntity.class);
            sendStats.setAccessible(true);
            sendStats.invoke(stats, player);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * 强制保存一个"空"的玩家数据到磁盘，覆盖原有数据
     */
    private static void forceSaveEmptyPlayerData(ServerPlayerEntity player, MinecraftServer server, UUID uuid) {
        try {
            // 先让游戏正常保存一次（此时玩家数据已被清空）
            PlayerManager pm = server.getPlayerManager();
            Method saveMethod = PlayerManager.class.getDeclaredMethod("savePlayerData", ServerPlayerEntity.class);
            saveMethod.setAccessible(true);
            saveMethod.invoke(pm, player);

            // 更暴力：直接写入空 NBT 覆盖文件
            Path playerDataPath = server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuid + ".dat");
            NbtCompound emptyNbt = new NbtCompound();
            NbtIo.writeCompressed(emptyNbt, playerDataPath.toFile());
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * 删除文件系统上的玩家存档文件
     */
    private static void deletePlayerFiles(MinecraftServer server, UUID uuid) {
        String uuidStr = uuid.toString();

        // 玩家数据
        deleteFile(server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuidStr + ".dat").toFile());
        deleteFile(server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuidStr + ".dat_old").toFile());

        // 进度
        Path advPath = server.getSavePath(WorldSavePath.ADVANCEMENTS);
        deleteFile(advPath.resolve(uuidStr + ".json").toFile());

        // 统计
        Path statsPath = server.getSavePath(WorldSavePath.STATS);
        deleteFile(statsPath.resolve(uuidStr + ".json").toFile());

        // 如果有 level.dat 里的 Player 数据（单人游戏），这里不处理，因为需要解析 level.dat
    }

    private static void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static void clearTrinkets(ServerPlayerEntity player) {
        TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
            component.getInventory().forEach((group, slots) -> {
                slots.forEach((slot, inventory) -> {
                    for (int i = 0; i < inventory.size(); i++) {
                        inventory.setStack(i, ItemStack.EMPTY);
                    }
                });
            });
        });
    }

    private static boolean hasProtectedItem(ServerPlayerEntity player) {
        // 检查背包
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).getItem() instanceof KeytoTime) return true;
        }
        // 检查末影箱
        for (int i = 0; i < player.getEnderChestInventory().size(); i++) {
            if (player.getEnderChestInventory().getStack(i).getItem() instanceof KeytoTime) return true;
        }
        // 检查 Trinket 饰品栏
        var trinketOpt = TrinketsApi.getTrinketComponent(player);
        if (trinketOpt.isPresent()) {
            for (var groupEntry : trinketOpt.get().getInventory().entrySet()) {
                for (var slotEntry : groupEntry.getValue().entrySet()) {
                    var inventory = slotEntry.getValue();
                    for (int i = 0; i < inventory.size(); i++) {
                        if (inventory.getStack(i).getItem() instanceof KeytoTime) return true;
                    }
                }
            }
        }
        return false;
    }
}