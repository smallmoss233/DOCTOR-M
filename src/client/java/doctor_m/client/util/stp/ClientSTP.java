package doctor_m.client.util.stp;

import doctor_m.mixin.client.stp.ClientPlayNetworkHandlerAccessor;
import doctor_m.mixin.client.stp.ClientWorldInvoker;
import doctor_m.module.STP;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端侧 STP 实现。
 *
 * <p>负责：
 * <ul>
 *   <li>接收 PRELOAD 包，把目标维度的区块数据缓存在内存里</li>
 *   <li>接收 TP 包，手动重建 ClientWorld 并从缓存中加载区块</li>
 *   <li>处理 UNLOAD / 断线清缓存</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class ClientSTP implements ClientModInitializer {

    /** 世界ID -> 预加载的区块数据集合。 */
    private static final Map<Identifier, Set<CompleteChunkData>> MAP = new ConcurrentHashMap<>();

    /**
     * 预加载区块数据。用 record 的自动 hashCode/equals（原 STP 手写的 hashCode 有碰撞风险）。
     */
    record CompleteChunkData(int x, int z, ChunkData chunkData, LightData lightData) {
        public CompleteChunkData(ChunkDataS2CPacket packet) {
            this(packet.getX(), packet.getZ(), packet.getChunkData(), packet.getLightData());
        }
    }

    @Override
    public void onInitializeClient() {
        // ---- TP 包：真正重建世界 ----
        ClientPlayNetworking.registerGlobalReceiver(STP.TP, (client, handler, buf, response) -> {
            RegistryKey<DimensionType> dimensionTypeKey = buf.readRegistryKey(RegistryKeys.DIMENSION_TYPE);
            RegistryKey<World> worldKey = buf.readRegistryKey(RegistryKeys.WORLD);
            long sha256Seed = buf.readLong();
            GameMode gameMode = GameMode.byId(buf.readUnsignedByte());
            GameMode previousGameMode = GameMode.getOrNull(buf.readByte());
            boolean debugWorld = buf.readBoolean();
            boolean flatWorld = buf.readBoolean();
            byte flag = buf.readByte();
            Optional<GlobalPos> lastDeathPos = buf.readOptional(PacketByteBuf::readGlobalPos);
            int portalCooldown = buf.readVarInt();

            client.executeSync(() -> {
                if (client.player == null || client.world == null) return;
                if (!(client.getNetworkHandler() instanceof ClientPlayNetworkHandlerAccessor accessor)) return;

                RegistryEntry.Reference<DimensionType> dimensionTypeEntry =
                        accessor.getCombinedDynamicRegistries()
                                .getCombinedRegistryManager()
                                .get(RegistryKeys.DIMENSION_TYPE)
                                .entryOf(dimensionTypeKey);

                tp(accessor, client, client.player, dimensionTypeEntry,
                        worldKey, sha256Seed, gameMode, previousGameMode,
                        debugWorld, flatWorld, flag, lastDeathPos, portalCooldown);
            });
        });

        // ---- PRELOAD 包：缓存区块 ----
        ClientPlayNetworking.registerGlobalReceiver(STP.PRELOAD, (client, handler, buf, sender) -> {
            Identifier id = buf.readIdentifier();
            ChunkDataS2CPacket packet = new ChunkDataS2CPacket(buf);

            MAP.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet())
                    .add(new CompleteChunkData(packet));
        });

        // ---- UNLOAD 包：清缓存 ----
        ClientPlayNetworking.registerGlobalReceiver(STP.UNLOAD, (client, handler, buf, sender) -> {
            Identifier id = buf.readIdentifier();
            Set<CompleteChunkData> set = MAP.remove(id);
            if (set != null) set.clear();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MAP.clear());
    }

    /**
     * 从缓存加载区块到指定 ClientWorld。
     * 每个区块独立 try-catch，避免单个坏块导致全部失败。
     *
     * @return 是否成功从缓存加载过
     */
    private static boolean tryLoadCache(ClientWorld world) {
        Set<CompleteChunkData> set = MAP.remove(world.getRegistryKey().getValue());
        if (set == null) return false;

        for (CompleteChunkData complete : set) {
            try {
                loadChunk(world, complete);
            } catch (Throwable t) {
                STP.LOGGER.error("Failed to load preloaded chunk ({},{})",
                        complete.x(), complete.z(), t);
            }
        }
        return true;
    }

    private static void loadChunk(ClientWorld world, CompleteChunkData complete) {
        int x = complete.x();
        int z = complete.z();
        ChunkData chunkData = complete.chunkData();
        LightData lightData = complete.lightData();

        world.getChunkManager().loadChunkFromPacket(
                x, z, chunkData.getSectionsDataBuf(),
                chunkData.getHeightmap(), chunkData.getBlockEntities(x, z));

        world.enqueueChunkUpdate(() -> {
            LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();

            updateLighting(world, x, z, lightingProvider, LightType.SKY,
                    lightData.getInitedSky(), lightData.getUninitedSky(),
                    lightData.getSkyNibbles().iterator());

            updateLighting(world, x, z, lightingProvider, LightType.BLOCK,
                    lightData.getInitedBlock(), lightData.getUninitedBlock(),
                    lightData.getBlockNibbles().iterator());

            lightingProvider.setColumnEnabled(new ChunkPos(x, z), true);

            WorldChunk worldChunk = world.getChunkManager().getWorldChunk(x, z, false);
            if (worldChunk != null) {
                ChunkSection[] sections = worldChunk.getSectionArray();
                ChunkPos chunkPos = worldChunk.getPos();

                for (int i = 0; i < sections.length; i++) {
                    ChunkSection section = sections[i];
                    int coord = world.sectionIndexToCoord(i);
                    lightingProvider.setSectionStatus(
                            ChunkSectionPos.from(chunkPos, coord), section.isEmpty());
                    world.scheduleBlockRenders(x, coord, z);
                }
            }
        });
    }

    private static void updateLighting(ClientWorld world, int chunkX, int chunkZ,
                                       LightingProvider provider, LightType type,
                                       BitSet inited, BitSet uninited, Iterator<byte[]> nibbles) {
        for (int i = 0; i < provider.getHeight(); i++) {
            int y = provider.getBottomY() + i;
            boolean init = inited.get(i);
            boolean uninit = uninited.get(i);
            if (!init && !uninit) continue;

            provider.enqueueSectionData(type, ChunkSectionPos.from(chunkX, y, chunkZ),
                    init ? new ChunkNibbleArray(nibbles.next().clone()) : new ChunkNibbleArray());
            world.scheduleBlockRenders(chunkX, y, chunkZ);
        }
    }

    private static void tp(ClientPlayNetworkHandlerAccessor accessor,
                           MinecraftClient client, ClientPlayerEntity player,
                           RegistryEntry.Reference<DimensionType> toDimensionEntry,
                           RegistryKey<World> toWorldKey, long sha256Seed,
                           GameMode gameMode, @Nullable GameMode previousGameMode,
                           boolean debugWorld, boolean flatWorld, byte flag,
                           Optional<GlobalPos> lastDeathPos, int portalCooldown) {

        ClientWorld world = accessor.getWorld();
        int entityId = player.getId();

        // ---- 维度切换：手动构造 ClientWorld ----
        if (toWorldKey != player.getWorld().getRegistryKey()) {
            ClientWorld.Properties oldProperties = accessor.getWorldProperties();
            ClientWorld.Properties newProperties = new ClientWorld.Properties(
                    oldProperties.getDifficulty(), oldProperties.isHardcore(), flatWorld);

            Scoreboard scoreboard = world.getScoreboard();
            Map<String, MapState> mapStates = ((ClientWorldInvoker) world).stp$getMapStates();

            accessor.setWorldProperties(newProperties);

            world = new ClientWorld(player.networkHandler, newProperties, toWorldKey,
                    toDimensionEntry, accessor.getChunkLoadDistance(),
                    accessor.getSimulationDistance(), client::getProfiler,
                    client.worldRenderer, debugWorld, sha256Seed);

            accessor.setWorld(world);

            world.setScoreboard(scoreboard);
            ((ClientWorldInvoker) world).stp$putMapStates(mapStates);

            ((STPMinecraftClient) client).stp$joinWorld(world);
        }

        // ---- 创建新的 ClientPlayerEntity ----
        String brand = player.getServerBrand();
        client.cameraEntity = null;

        if (player.shouldCloseHandledScreenOnRespawn()) player.closeHandledScreen();

        ClientPlayerEntity newPlayer = (flag & 2) != 0
                ? client.interactionManager.createPlayer(world, player.getStatHandler(),
                player.getRecipeBook(), player.isSneaking(), player.isSprinting())
                : client.interactionManager.createPlayer(world, player.getStatHandler(), player.getRecipeBook());

        newPlayer.setId(entityId);
        client.player = newPlayer;

        if (toWorldKey != player.getWorld().getRegistryKey()) client.getMusicTracker().stop();

        client.cameraEntity = newPlayer;

        // ---- 状态迁移 ----
        List<DataTracker.SerializedEntry<?>> entries = player.getDataTracker().getChangedEntries();
        if ((flag & 2) != 0 && entries != null) newPlayer.getDataTracker().writeUpdatedEntries(entries);
        if ((flag & 1) != 0) newPlayer.getAttributes().setFrom(player.getAttributes());

        newPlayer.init();
        newPlayer.setServerBrand(brand);
        world.addPlayer(entityId, newPlayer);
        newPlayer.setYaw(-180.0f);
        newPlayer.input = new KeyboardInput(client.options);

        client.interactionManager.copyAbilities(newPlayer);

        newPlayer.setReducedDebugInfo(player.hasReducedDebugInfo());
        newPlayer.setShowsDeathScreen(player.showsDeathScreen());
        newPlayer.setLastDeathPos(lastDeathPos);
        newPlayer.setPortalCooldown(portalCooldown);

        newPlayer.nauseaIntensity = player.nauseaIntensity;
        newPlayer.prevNauseaIntensity = player.prevNauseaIntensity;

        if (client.currentScreen instanceof DeathScreen
                || client.currentScreen instanceof DeathScreen.TitleScreenConfirmScreen) {
            client.setScreen(null);
        }

        client.interactionManager.setGameModes(gameMode, previousGameMode);

        // ---- 世界渲染器：优先走缓存路径 ----
        if (tryLoadCache(world)) {
            ((STPWorldRenderer) client.worldRenderer).stp$setWorld(world);
        } else {
            client.worldRenderer.setWorld(world);
        }
    }
}