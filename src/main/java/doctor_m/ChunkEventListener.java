package doctor_m.worldgen;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkEventListener");

    // 跟踪每个世界需要检查的区块
    private static final Map<String, Set<ChunkPos>> worldsToCheck = new HashMap<>();
    private static final ChunkPos STRUCTURE_CHUNK = new ChunkPos(6, 6); // (100,100)在区块(6,6)

    /**
     * 初始化监听器
     */
    public static void initialize() {
        // 监听世界加载
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                String worldId = world.getRegistryKey().getValue().toString();

                // 标记需要检查这个区块
                Set<ChunkPos> chunks = new HashSet<>();
                chunks.add(STRUCTURE_CHUNK);
                worldsToCheck.put(worldId, chunks);
            }
        });

        // 监听服务器tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            checkAndGenerateStructures(server);
        });
    }

    /**
     * 检查并生成结构
     */
    private static void checkAndGenerateStructures(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey() != World.OVERWORLD) continue;

            String worldId = world.getRegistryKey().getValue().toString();
            Set<ChunkPos> chunks = worldsToCheck.get(worldId);

            if (chunks != null && !chunks.isEmpty()) {
                // 检查目标区块是否已加载
                for (ChunkPos chunkPos : chunks) {
                    if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                        // 尝试生成结构
                        boolean generated = doctor_m.worldgen.ArchonHouseGenerator.checkAndGenerate(world);

                        // 如果生成成功或永久失败，从检查列表中移除
                        if (generated) {
                            chunks.remove(chunkPos);
                            LOGGER.debug("在区块 {} 生成结构成功", chunkPos);
                        }

                        // 如果已经没有区块需要检查，移除这个世界
                        if (chunks.isEmpty()) {
                            worldsToCheck.remove(worldId);
                        }

                        break; // 每次只处理一个区块
                    }
                }
            }
        }
    }
}