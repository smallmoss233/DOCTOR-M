package doctor_m.handler;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.KeytoTime;
import doctor_m.Item.data_item.DeMatGunItem;
import doctor_m.api.ModSounds;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.StatHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * De-Mat Gun
 * 设计目标：
 * 1. 不持有 KeytoTime 的玩家，无论携带何种神器，都会被彻底抹除数据。
 * 2. 保留网络连接，让玩家走正常死亡→重生流程，重生后数据为空（近似新玩家）。
 * 3. 若死亡被模组拦截，则将其转化为"时间幽灵"（无法与世界交互）。
 * 4. 执行顺序：冻结 → 清空所有数据 → 存档抹除 → 处决(kill) → 清理内部缓存 → 幽灵化保险。
 */
public class DeMatGunEntityEraser {

    private static final Vector3f GOLD_COLOR = new Vector3f(1.0f, 0.84f, 0.0f);
    private static final float BEAM_PARTICLE_SIZE = 1.0f;
    private static final float ERASE_PARTICLE_SIZE = 1.5f;

    public static void eraseByRaycast(PlayerEntity shooter, World world, double range) {

        ItemStack gunStack = shooter.getMainHandStack();
        if (!(gunStack.getItem() instanceof DeMatGunItem)) {
            gunStack = shooter.getOffHandStack();
            if (!(gunStack.getItem() instanceof DeMatGunItem)) {
                return; // 没有持枪，直接返回
            }
        }
        DeMatGunItem gunItem = (DeMatGunItem) gunStack.getItem();
        if (!gunItem.isAuthorized(gunStack)) {
            if (shooter instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                        Text.translatable("message.doctor_m.de_mat_gun.not_authorized")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return;
        }

        Vec3d start = shooter.getEyePos();
        Vec3d direction = shooter.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(range));

        spawnBeamParticles(world, start, end, range);

        world.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                ModSounds.DE_MAT_GUN_FIRE, SoundCategory.PLAYERS, 1.0f, 1.0f);

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
                    ModSounds.DE_MAT_GUN_ERASE, SoundCategory.PLAYERS, 1.0f, 1.0f);

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
     * 从时间线上彻底抹除玩家。保留连接，让玩家走正常死亡→重生流程。
     */
    private static void erasePlayerFromTimeline(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        String name = player.getEntityName();

        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.setNoGravity(true);
        player.setInvulnerable(true);
        clearEntityTags(player);

        player.clearStatusEffects();
        player.setAbsorptionAmount(0.0f);
        player.setAir(player.getMaxAir());
        player.setFireTicks(0);
        player.fallDistance = 0.0f;
        player.setGlowing(false);
        player.setSilent(false);
        player.setCustomName(null);
        player.setCustomNameVisible(false);
        player.setSneaking(false);
        player.setSprinting(false);

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
        clearScoreboard(server, name);

        clearEntityTags(player);

        forceSaveEmptyPlayerData(player, server, uuid);
        deletePlayerFiles(server, uuid);

        player.kill();

        purgeFromWorldSystems(player, server);
        purgePlayerManagerCaches(player, server);

        if (player.isAlive()) {
            turnIntoTimeGhost(player);
        }
    }

    private static void clearAdvancements(ServerPlayerEntity player) {
        try {
            Object tracker = player.getAdvancementTracker();
            Field progressField = tracker.getClass().getDeclaredField("advancementToProgress");
            progressField.setAccessible(true);
            Map<?, ?> progressMap = (Map<?, ?>) progressField.get(tracker);
            if (progressMap != null) progressMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除玩家配方书。不直接引用 ServerRecipeBook / RecipeBook 类，避免映射差异。
     */
    private static void clearRecipes(ServerPlayerEntity player) {
        try {
            Object recipeBook = player.getRecipeBook();
            Class<?> bookClass = recipeBook.getClass();

            Field recipesField = getFieldFromHierarchy(bookClass, "recipes");
            if (recipesField != null) {
                recipesField.setAccessible(true);
                Object recipes = recipesField.get(recipeBook);
                if (recipes instanceof Set<?>) ((Set<?>) recipes).clear();
            }

            Field displayedField = getFieldFromHierarchy(bookClass, "toBeDisplayed");
            if (displayedField == null) {
                displayedField = getFieldFromHierarchy(bookClass, "displayedRecipes");
            }
            if (displayedField != null) {
                displayedField.setAccessible(true);
                Object displayed = displayedField.get(recipeBook);
                if (displayed instanceof Set<?>) ((Set<?>) displayed).clear();
            }

            try {
                Method sendInit = bookClass.getDeclaredMethod("sendInitRecipesPacket", ServerPlayerEntity.class);
                sendInit.setAccessible(true);
                sendInit.invoke(recipeBook, player);
            } catch (Exception ex1) {
                try {
                    Method sendInit = bookClass.getDeclaredMethod("sendUnlockRecipes", ServerPlayerEntity.class);
                    sendInit.setAccessible(true);
                    sendInit.invoke(recipeBook, player);
                } catch (Exception ex2) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearStats(ServerPlayerEntity player) {
        try {
            StatHandler stats = player.getStatHandler();
            Field statMapField = StatHandler.class.getDeclaredField("statMap");
            statMapField.setAccessible(true);
            Object statMap = statMapField.get(stats);
            if (statMap instanceof Map<?, ?>) ((Map<?, ?>) statMap).clear();

            Method sendStats = StatHandler.class.getDeclaredMethod("sendStats", ServerPlayerEntity.class);
            sendStats.setAccessible(true);
            sendStats.invoke(stats, player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearScoreboard(MinecraftServer server, String playerName) {
        try {
            Scoreboard scoreboard = server.getScoreboard();
            Field stateField = Scoreboard.class.getDeclaredField("state");
            stateField.setAccessible(true);
            Object state = stateField.get(scoreboard);
            if (state != null) {
                Field scoresField = state.getClass().getDeclaredField("playerScores");
                scoresField.setAccessible(true);
                Map<?, ?> scores = (Map<?, ?>) scoresField.get(state);
                if (scores != null) scores.remove(playerName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除实体计分板标签。兼容 Yarn (getScoreboardTags) 和 Mojang (getTags) 映射。
     */
    private static void clearEntityTags(Entity entity) {
        try {
            // Yarn 1.20.1 标准方式
            Method getTagsMethod = Entity.class.getMethod("getScoreboardTags");
            Set<?> tags = (Set<?>) getTagsMethod.invoke(entity);
            if (tags != null) tags.clear();
        } catch (Exception e) {
            try {
                // Mojang / Parchment 映射回退
                Method getTagsMethod = Entity.class.getMethod("getTags");
                Set<?> tags = (Set<?>) getTagsMethod.invoke(entity);
                if (tags != null) tags.clear();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void forceSaveEmptyPlayerData(ServerPlayerEntity player, MinecraftServer server, UUID uuid) {
        try {
            PlayerManager pm = server.getPlayerManager();
            Method saveMethod = PlayerManager.class.getDeclaredMethod("savePlayerData", ServerPlayerEntity.class);
            saveMethod.setAccessible(true);
            saveMethod.invoke(pm, player);

            Path playerDataPath = server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuid + ".dat");
            NbtCompound emptyNbt = new NbtCompound();
            NbtIo.writeCompressed(emptyNbt, playerDataPath.toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deletePlayerFiles(MinecraftServer server, UUID uuid) {
        String uuidStr = uuid.toString();
        deleteFile(server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuidStr + ".dat").toFile());
        deleteFile(server.getSavePath(WorldSavePath.PLAYERDATA).resolve(uuidStr + ".dat_old").toFile());

        Path advPath = server.getSavePath(WorldSavePath.ADVANCEMENTS);
        deleteFile(advPath.resolve(uuidStr + ".json").toFile());

        Path statsPath = server.getSavePath(WorldSavePath.STATS);
        deleteFile(statsPath.resolve(uuidStr + ".json").toFile());
    }

    private static void deleteFile(File file) {
        if (file != null && file.exists() && !file.delete()) {
            System.err.println("[DeMatGun] Failed to delete file: " + file.getAbsolutePath());
        }
    }

    private static void clearTrinkets(ServerPlayerEntity player) {
        try {
            TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                component.getInventory().forEach((group, slots) -> {
                    slots.forEach((slot, inventory) -> {
                        for (int i = 0; i < inventory.size(); i++) {
                            inventory.setStack(i, ItemStack.EMPTY);
                        }
                    });
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void purgeFromWorldSystems(ServerPlayerEntity player, MinecraftServer server) {
        try {
            ServerWorld world = player.getServerWorld();

            try {
                Field playersField = ServerWorld.class.getDeclaredField("players");
                playersField.setAccessible(true);
                List<?> players = (List<?>) playersField.get(world);
                if (players != null) players.remove(player);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Field entityManagerField = ServerWorld.class.getDeclaredField("entityManager");
                entityManagerField.setAccessible(true);
                Object entityManager = entityManagerField.get(world);
                Method removeMethod = entityManager.getClass().getDeclaredMethod("remove", Entity.class);
                removeMethod.invoke(entityManager, player);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Field chunkManagerField = ServerWorld.class.getDeclaredField("chunkManager");
                chunkManagerField.setAccessible(true);
                Object chunkManager = chunkManagerField.get(world);
                Field entityTrackersField = chunkManager.getClass().getDeclaredField("entityTrackers");
                entityTrackersField.setAccessible(true);
                Map<?, ?> trackers = (Map<?, ?>) entityTrackersField.get(chunkManager);
                if (trackers != null) trackers.remove(player.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void purgePlayerManagerCaches(ServerPlayerEntity player, MinecraftServer server) {
        try {
            PlayerManager pm = server.getPlayerManager();
            UUID uuid = player.getUuid();

            try {
                Field statisticsField = PlayerManager.class.getDeclaredField("statistics");
                statisticsField.setAccessible(true);
                Map<?, ?> statisticsMap = (Map<?, ?>) statisticsField.get(pm);
                if (statisticsMap != null) statisticsMap.remove(uuid);
            } catch (Exception e) {
                try {
                    Field statisticsField = PlayerManager.class.getDeclaredField("statisticsMap");
                    statisticsField.setAccessible(true);
                    Map<?, ?> statisticsMap = (Map<?, ?>) statisticsField.get(pm);
                    if (statisticsMap != null) statisticsMap.remove(uuid);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            try {
                Field advancementsField = PlayerManager.class.getDeclaredField("advancementTrackers");
                advancementsField.setAccessible(true);
                Map<?, ?> advancementTrackers = (Map<?, ?>) advancementsField.get(pm);
                if (advancementTrackers != null) advancementTrackers.remove(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void turnIntoTimeGhost(ServerPlayerEntity player) {
        System.err.println("[DeMatGun] " + player.getEntityName() + " resisted death — converting to Time Ghost");

        player.noClip = true;
        player.setInvulnerable(true);
        player.setInvisible(true);
        player.setSilent(true);
        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.setFireTicks(0);
        player.setGlowing(false);
        clearEntityTags(player);

        player.getInventory().clear();
        player.getEnderChestInventory().clear();
        clearTrinkets(player);
        player.clearStatusEffects();
    }

    /**
     * 从类层次结构中查找字段（当前类 → 父类 → ...）
     */
    private static Field getFieldFromHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static boolean hasProtectedItem(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).getItem() instanceof KeytoTime) return true;
        }
        for (int i = 0; i < player.getEnderChestInventory().size(); i++) {
            if (player.getEnderChestInventory().getStack(i).getItem() instanceof KeytoTime) return true;
        }
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