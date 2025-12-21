package doctor_m.worldgen;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ArchonHouseGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("ArchonHouseGenerator");

    // 固定XZ位置
    private static final BlockPos STRUCTURE_POS = new BlockPos(100, 0, 100);

    // 自适应高度参数
    private static final int MIN_HEIGHT = 64;     // 最低高度
    private static final int MAX_HEIGHT = 120;    // 最高高度（给高山足够的空间）

    // 使用持久数据标记，避免Map跨世界问题
    private static final String GENERATED_KEY = "archon_house_generated";

    /**
     * 检查并生成结构
     */
    public static boolean checkAndGenerate(ServerWorld world) {
        try {
            // 检查是否已经生成过（使用持久数据）
            if (hasGenerated(world)) {
                return false;
            }

            // 获取结构文件
            StructureTemplateManager manager = world.getStructureTemplateManager();
            Optional<StructureTemplate> template = manager.getTemplate(
                    new Identifier("doctor_m", "house_of_archon")
            );

            if (template.isEmpty()) {
                return false;
            }

            // 生成结构
            generateStructure(world, template.get());

            // 标记为已生成
            markAsGenerated(world);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 自适应地形生成结构
     */
    private static void generateStructure(ServerWorld world, StructureTemplate structure) {
        // 1. 扫描区域以获取最佳高度
        int bestHeight = findBestHeight(world, STRUCTURE_POS, structure);

        // 2. 准备地形（自适应地形）
        prepareTerrain(world, STRUCTURE_POS, bestHeight, structure);

        // 3. 放置结构
        Vec3i size = structure.getSize();
        BlockPos placePos = new BlockPos(
                STRUCTURE_POS.getX() - size.getX() / 2,
                bestHeight,
                STRUCTURE_POS.getZ() - size.getZ() / 2
        );

        structure.place(world, placePos, placePos,
                new net.minecraft.structure.StructurePlacementData()
                        .setUpdateNeighbors(true)
                        .setIgnoreEntities(false),
                world.random, 3);
    }

    /**
     * 寻找最佳高度（自适应地形）
     */
    private static int findBestHeight(ServerWorld world, BlockPos center, StructureTemplate structure) {
        Vec3i size = structure.getSize();
        int padding = 3;

        int totalHeight = 0;
        int sampleCount = 0;
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        // 扫描结构区域，获取高度范围
        for (int x = -size.getX()/2 - padding; x <= size.getX()/2 + padding; x += 2) {
            for (int z = -size.getZ()/2 - padding; z <= size.getZ()/2 + padding; z += 2) {
                int worldX = center.getX() + x;
                int worldZ = center.getZ() + z;

                // 使用WORLD_SURFACE_WG获取真实地表高度
                int height = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, worldX, worldZ);

                totalHeight += height;
                sampleCount++;

                if (height < minHeight) minHeight = height;
                if (height > maxHeight) maxHeight = height;
            }
        }

        // 计算平均高度
        int averageHeight = sampleCount > 0 ? totalHeight / sampleCount : MIN_HEIGHT;

        // 限制高度范围
        int finalHeight = Math.max(MIN_HEIGHT, Math.min(averageHeight, MAX_HEIGHT));

        // 如果是陡峭地形（高差大），选择较低高度以保证结构稳定
        if (maxHeight - minHeight > 10) {
            finalHeight = minHeight + 2; // 在最低点加2格
        }

        return finalHeight;
    }

    /**
     * 准备地形（自适应地形）
     */
    private static void prepareTerrain(ServerWorld world, BlockPos center, int targetHeight, StructureTemplate structure) {
        Vec3i size = structure.getSize();
        int padding = 3;

        // 清理和填充区域
        for (int x = -size.getX()/2 - padding; x <= size.getX()/2 + padding; x++) {
            for (int z = -size.getZ()/2 - padding; z <= size.getZ()/2 + padding; z++) {
                int worldX = center.getX() + x;
                int worldZ = center.getZ() + z;
                int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, worldX, worldZ);

                BlockPos currentPos = new BlockPos(worldX, surfaceY, worldZ);
                BlockState currentBlock = world.getBlockState(currentPos);

                // 确定合适的填充方块（基于当前地形）
                BlockState fillBlock = determineFillBlock(world, currentBlock, currentPos);

                // 自适应填充：如果当前位置低于目标高度，填充
                if (surfaceY < targetHeight) {
                    for (int y = surfaceY; y < targetHeight; y++) {
                        BlockPos fillPos = new BlockPos(worldX, y, worldZ);

                        // 底层用石头，上层用合适方块
                        BlockState blockToPlace = (y == targetHeight - 1 && y - surfaceY < 3) ?
                                getTopBlock(world, fillPos) : fillBlock;

                        world.setBlockState(fillPos, blockToPlace, 3);
                    }
                }
                // 如果当前位置高于目标高度，只清理突出部分
                else if (surfaceY > targetHeight + size.getY()) {
                    for (int y = targetHeight + size.getY(); y <= surfaceY; y++) {
                        BlockPos clearPos = new BlockPos(worldX, y, worldZ);
                        world.setBlockState(clearPos, Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
        }
    }

    /**
     * 根据当前地形确定填充方块
     */
    private static BlockState determineFillBlock(ServerWorld world, BlockState currentBlock, BlockPos pos) {
        // 根据当前方块类型选择填充方块
        if (currentBlock.isOf(Blocks.GRASS_BLOCK) || currentBlock.isOf(Blocks.DIRT)) {
            return Blocks.DIRT.getDefaultState();
        } else if (currentBlock.isOf(Blocks.STONE) || currentBlock.isOf(Blocks.DEEPSLATE)) {
            return Blocks.STONE.getDefaultState();
        } else if (currentBlock.isOf(Blocks.SAND) || currentBlock.isOf(Blocks.SANDSTONE)) {
            return Blocks.SANDSTONE.getDefaultState();
        } else if (currentBlock.isOf(Blocks.SNOW_BLOCK) || currentBlock.isOf(Blocks.ICE)) {
            return Blocks.SNOW_BLOCK.getDefaultState();
        } else {
            // 默认使用石头
            return Blocks.STONE.getDefaultState();
        }
    }

    /**
     * 获取顶部方块
     */
    private static BlockState getTopBlock(ServerWorld world, BlockPos pos) {
        // 根据生物群系选择顶部方块
        RegistryKey<World> dimension = world.getRegistryKey();

        if (dimension == World.OVERWORLD) {
            // 在水域附近使用沙砾
            if (isNearWater(world, pos)) {
                return Blocks.GRAVEL.getDefaultState();
            }
            // 默认使用草地
            return Blocks.GRASS_BLOCK.getDefaultState();
        }

        return Blocks.DIRT.getDefaultState();
    }

    /**
     * 检查是否靠近水域
     */
    private static boolean isNearWater(ServerWorld world, BlockPos pos) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                if (world.getBlockState(checkPos).isOf(Blocks.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查是否已经生成过（使用持久数据）
     */
    private static boolean hasGenerated(ServerWorld world) {
        // 使用简单的方块标记：在结构位置检查特定方块
        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG,
                STRUCTURE_POS.getX(), STRUCTURE_POS.getZ());
        BlockPos markerPos = STRUCTURE_POS.withY(surfaceY);

        // 如果已经放置了标记方块，则认为已经生成过
        return world.getBlockState(markerPos).isOf(Blocks.COBBLESTONE) ||
                world.getBlockState(markerPos.down()).isOf(Blocks.COBBLESTONE);
    }

    /**
     * 标记为已生成
     */
    private static void markAsGenerated(ServerWorld world) {
        // 在结构下方放置一个隐蔽的标记方块
        int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG,
                STRUCTURE_POS.getX(), STRUCTURE_POS.getZ());
        BlockPos markerPos = STRUCTURE_POS.withY(surfaceY).down(3);

        world.setBlockState(markerPos, Blocks.COBBLESTONE.getDefaultState(), 3);
    }
}