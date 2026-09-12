package doctor_m.module;

import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.mixin.stp.EntityInvoker;
import doctor_m.mixin.stp.ServerPlayerEntityInvoker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端侧 STP（Seamless Teleport）主入口。
 *
 * <p>核心思想：在真正切换世界之前，把目标维度的周边区块数据通过自定义包提前推给客户端，
 * 客户端把区块缓存在内存里。传送时只发一个元信息包，客户端本地重建 ClientWorld 并从缓存
 * 直接灌入区块，从而完全绕过原版 PlayerRespawnS2CPacket 触发的加载屏幕。
 *
 * <p>优化点：
 * <ul>
 *     <li>异步加载区块：避免 world.getChunk 阻塞主线程</li>
 *     <li>预加载半径 1（3x3=9 个区块），减少数据量与耗时</li>
 *     <li>去重窗口：5 秒内同一中心复用同一个 future，避免重复加载</li>
 *     <li>断线检查：玩家离线后不再发包</li>
 *     <li>配置开关：可通过 ModConfig 完全禁用 STP</li>
 * </ul>
 */
public class STP implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("doctor_m_stp");

    public static final Identifier PRELOAD = new Identifier("doctor_m", "stp_preload");
    public static final Identifier UNLOAD  = new Identifier("doctor_m", "stp_unload");
    public static final Identifier TP      = new Identifier("doctor_m", "stp_tp");

    private static final int PRELOAD_RADIUS = 1;
    private static final long PRELOAD_DEDUP_WINDOW_MS = 5000L;

    /** 每个玩家最近一次的预加载任务。 */
    private static final Map<UUID, PendingPreload> PENDING = new ConcurrentHashMap<>();

    record PendingPreload(ChunkPos center, CompletableFuture<Void> future, long timestamp) {}

    private static ModConfig config;

    private static ModConfig config() {
        if (config == null) {
            config = ConfigManager.getConfig();
        }
        return config;
    }

    public static boolean isEnabled() {
        return config().seamlessTeleportEnabled;
    }

    public static void invalidateCache() {
        config = null;
    }

    // ==================== 对外 API ====================

    /**
     * 兼容旧签名的预加载，不返回 future。内部调用 {@link #preloadAllAsync}。
     */
    public static void preloadAll(ServerPlayerEntity player, ServerWorld world, ChunkPos origin) {
        preloadAllAsync(player, world, origin);
    }

    /**
     * 异步预加载，返回一个 CompletableFuture。
     *
     * <p>同一中心的重复调用会返回同一个 future，不会重复触发预加载。
     * 调用方可以链式 {@code .thenRun(...)} 等待预加载完成后再执行传送，
     * 避免传送时客户端还没收到区块缓存导致 STP 失效。
     */
    public static CompletableFuture<Void> preloadAllAsync(ServerPlayerEntity player,
                                                          ServerWorld world,
                                                          ChunkPos origin) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();

        // 去重：同一中心且 5 秒内的请求，复用现有 future
        PendingPreload pending = PENDING.get(uuid);
        if (pending != null
                && pending.center().equals(origin)
                && (now - pending.timestamp()) < PRELOAD_DEDUP_WINDOW_MS) {
            LOGGER.debug("Reusing pending preload for {} at {}",
                    player.getName().getString(), origin);
            return pending.future();
        }

        // 启动新预加载
        CompletableFuture<Void> future = new CompletableFuture<>();
        PENDING.put(uuid, new PendingPreload(origin, future, now));

        // 收集目标区块
        List<ChunkPos> targets = new ArrayList<>(
                (PRELOAD_RADIUS * 2 + 1) * (PRELOAD_RADIUS * 2 + 1));
        for (int offsetX = -PRELOAD_RADIUS; offsetX <= PRELOAD_RADIUS; offsetX++) {
            for (int offsetZ = -PRELOAD_RADIUS; offsetZ <= PRELOAD_RADIUS; offsetZ++) {
                targets.add(new ChunkPos(origin.x + offsetX, origin.z + offsetZ));
            }
        }

        // 异步加载区块
        CompletableFuture.runAsync(() -> {
            for (ChunkPos pos : targets) {
                try {
                    world.getChunk(pos.x, pos.z);
                } catch (Throwable t) {
                    LOGGER.warn("Failed to load chunk ({},{}) for preload", pos.x, pos.z, t);
                }
            }
        }).thenRun(() -> {
            MinecraftServer server = world.getServer();
            if (server == null) {
                future.complete(null);
                return;
            }

            // 回到主线程发包
            server.execute(() -> {
                try {
                    if (player.isDisconnected()) return;

                    unload(player, world);
                    int sent = 0;
                    for (ChunkPos pos : targets) {
                        try {
                            preload(player, world, pos);
                            sent++;
                        } catch (Throwable t) {
                            LOGGER.warn("Failed to send preload for ({},{})", pos.x, pos.z, t);
                        }
                    }
                    LOGGER.debug("Preloaded {} of {} chunks for {}",
                            sent, targets.size(), player.getName().getString());
                } finally {
                    future.complete(null);
                }
            });
        });

        return future;
    }

    public static void unload(ServerPlayerEntity player, ServerWorld world) {
        if (!isEnabled()) return;

        LOGGER.debug("Cancelling preload for {}", world.getRegistryKey().getValue());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(world.getRegistryKey().getValue());
        ServerPlayNetworking.send(player, UNLOAD, buf);
    }

    public static void teleport(ServerPlayerEntity player, ServerWorld targetWorld,
                                Vec3d pos, float yaw, float pitch) {
        if (!isEnabled()) {
            player.teleport(targetWorld, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
            return;
        }

        if (player.getWorld() == targetWorld) {
            player.teleport(targetWorld, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
            return;
        }

        LOGGER.debug("Teleporting {} to world {} at {}",
                player.getName().getString(), targetWorld.getRegistryKey().getValue(), pos);

        player.setCameraEntity(player);
        player.stopRiding();

        ServerWorld serverWorld = player.getServerWorld();
        WorldProperties worldProperties = targetWorld.getLevelProperties();

        sendTpPacket(player, targetWorld);
        player.networkHandler.sendPacket(
                new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
        player.server.getPlayerManager().sendCommandTree(player);

        serverWorld.removePlayer(player, Entity.RemovalReason.CHANGED_DIMENSION);
        ((EntityInvoker) player).stp$unsetRemoved();

        player.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
        player.setServerWorld(targetWorld);

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.invoker()
                .afterChangeWorld(player, serverWorld, targetWorld);

        targetWorld.onPlayerTeleport(player);
        ((ServerPlayerEntityInvoker) player).stp$worldChanged(serverWorld);

        player.networkHandler.requestTeleport(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
        player.server.getPlayerManager().sendWorldInfo(player, targetWorld);
        player.server.getPlayerManager().sendPlayerStatus(player);
    }

    public static void moveToWorld(ServerPlayerEntity player, ServerWorld destination) {
        if (!isEnabled()) {
            player.moveToWorld(destination);
            return;
        }

        LOGGER.debug("Moving {} to world {}",
                player.getName().getString(), destination.getRegistryKey().getValue());

        ((ServerPlayerEntityInvoker) player).setInTeleportationState(true);

        ServerWorld serverWorld = player.getServerWorld();
        WorldProperties worldProperties = destination.getLevelProperties();

        sendTpPacket(player, destination);
        player.networkHandler.sendPacket(
                new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));

        PlayerManager playerManager = player.getServer().getPlayerManager();
        playerManager.sendCommandTree(player);

        serverWorld.removePlayer(player, Entity.RemovalReason.CHANGED_DIMENSION);
        ((EntityInvoker) player).stp$unsetRemoved();

        TeleportTarget teleportTarget = ((ServerPlayerEntityInvoker) player).stp$getTeleportTarget(destination);
        if (teleportTarget == null) {
            LOGGER.warn("No teleport target for {} in {}", player.getName().getString(),
                    destination.getRegistryKey().getValue());
            return;
        }

        player.setServerWorld(destination);
        player.networkHandler.requestTeleport(
                teleportTarget.position.x, teleportTarget.position.y, teleportTarget.position.z,
                teleportTarget.yaw, teleportTarget.pitch);
        player.networkHandler.syncWithPlayerPosition();

        destination.onPlayerChangeDimension(player);
        ((ServerPlayerEntityInvoker) player).stp$worldChanged(serverWorld);
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));

        playerManager.sendWorldInfo(player, destination);
        playerManager.sendPlayerStatus(player);

        for (StatusEffectInstance effect : player.getStatusEffects()) {
            player.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getId(), effect));
        }

        player.networkHandler.sendPacket(
                new WorldEventS2CPacket(WorldEvents.TRAVEL_THROUGH_PORTAL, BlockPos.ORIGIN, 0, false));

        ((ServerPlayerEntityInvoker) player).setSyncedExperience(-1);
        ((ServerPlayerEntityInvoker) player).setSyncedHealth(-1.0f);
        ((ServerPlayerEntityInvoker) player).setSyncedFoodLevel(-1);
    }

    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        PENDING.remove(player.getUuid());
    }

    // ==================== 内部方法 ====================

    private static void preload(ServerPlayerEntity player, ServerWorld world, ChunkPos pos) {
        WorldChunk chunk = world.getChunk(pos.x, pos.z);
        ServerLightingProvider provider = world.getChunkManager().getLightingProvider();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(world.getRegistryKey().getValue());
        new ChunkDataS2CPacket(chunk, provider, null, null).write(buf);

        ServerPlayNetworking.send(player, PRELOAD, buf);
    }

    private static void sendTpPacket(ServerPlayerEntity player, ServerWorld targetWorld) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeRegistryKey(targetWorld.getDimensionKey());
        buf.writeRegistryKey(targetWorld.getRegistryKey());
        buf.writeLong(BiomeAccess.hashSeed(targetWorld.getSeed()));
        buf.writeByte(player.interactionManager.getGameMode().getId());
        buf.writeByte(GameMode.getId(player.interactionManager.getPreviousGameMode()));
        buf.writeBoolean(targetWorld.isDebugWorld());
        buf.writeBoolean(targetWorld.isFlat());
        buf.writeByte(3);
        buf.writeOptional(player.getLastDeathPos(), PacketByteBuf::writeGlobalPos);
        buf.writeVarInt(player.getPortalCooldown());
        ServerPlayNetworking.send(player, TP, buf);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("STP initialized (namespace: doctor_m, radius={})", PRELOAD_RADIUS);
    }
}