package doctor_m.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TitanChunkGenerator extends ChunkGenerator {

    public static final Codec<TitanChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(TitanChunkGenerator::getBiomeSource),
                    ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(g -> g.settings)
            ).apply(instance, TitanChunkGenerator::new)
    );

    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final NoiseChunkGenerator delegate;

    // 噪声种子偏移（实际应在构造时从 World 获取 seed）
    private static final long SEED_SALT = 0x544954414E4C4F56L; // "TITANLOV"

    public TitanChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.delegate = new NoiseChunkGenerator(biomeSource, settings);
    }

    @Override
    public Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    // === 抽象方法委托 ===

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        return delegate.populateNoise(executor, blender, noiseConfig, structureAccessor, chunk);
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return delegate.getColumnSample(x, z, world, noiseConfig);
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return delegate.getHeight(x, z, heightmap, world, noiseConfig);
    }

    @Override
    public CompletableFuture<Chunk> populateBiomes(Executor executor, NoiseConfig noiseConfig, Blender blender,
                                                   StructureAccessor structureAccessor, Chunk chunk) {
        return delegate.populateBiomes(executor, noiseConfig, blender, structureAccessor, chunk);
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        delegate.populateEntities(region);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        delegate.getDebugHudText(text, noiseConfig, pos);
    }

    @Override
    public int getWorldHeight() {
        return delegate.getWorldHeight();
    }

    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }

    @Override
    public int getMinimumY() {
        return delegate.getMinimumY();
    }

    @Override
    public int getSpawnHeight(HeightLimitView world) {
        return delegate.getSpawnHeight(world);
    }

    // === 禁洞穴 ===

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // 不生成洞穴，保持土卫六地质结构完整
    }

    // === 自定义地表 ===

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // 多层噪声组合，创造更自然的地形
                double baseNoise = sampleBaseNoise(worldX, worldZ);
                double detailNoise = sampleDetailNoise(worldX, worldZ);
                double lakeNoise = sampleLakeNoise(worldX, worldZ);

                int height = (int) (60 + baseNoise * 8 + detailNoise * 3);
                height = MathHelper.clamp(height, 48, 90);

                boolean isLake = lakeNoise > 0.6 && height < 65; // 低洼处形成湖泊
                int waterLevel = 62;

                // 基岩层
                for (int y = getMinimumY(); y < getMinimumY() + 5; y++) {
                    chunk.setBlockState(new BlockPos(worldX, y, worldZ), Blocks.BEDROCK.getDefaultState(), false);
                }

                // 深层：深板岩 + 偶尔玄武岩（模拟土卫六地下海洋接触带）
                int deepslateBottom = getMinimumY() + 5;
                int stoneBottom = height - 12;
                for (int y = deepslateBottom; y < stoneBottom; y++) {
                    BlockState state = (y < deepslateBottom + 3 && noise(worldX, y, worldZ) > 0.5)
                            ? Blocks.BASALT.getDefaultState()
                            : Blocks.DEEPSLATE.getDefaultState();
                    chunk.setBlockState(new BlockPos(worldX, y, worldZ), state, false);
                }

                // 中层：石头 + 偶尔安山岩/闪长岩（地质层）
                for (int y = stoneBottom; y < height - 3; y++) {
                    double rockNoise = noise(worldX * 0.1, y * 0.1, worldZ * 0.1);
                    BlockState state;
                    if (rockNoise > 0.7) {
                        state = Blocks.ANDESITE.getDefaultState();
                    } else if (rockNoise < -0.7) {
                        state = Blocks.DIORITE.getDefaultState();
                    } else {
                        state = Blocks.STONE.getDefaultState();
                    }
                    chunk.setBlockState(new BlockPos(worldX, y, worldZ), state, false);
                }

                // 表层：冰层系统
                if (isLake) {
                    // 液态甲烷湖
                    for (int y = height; y <= waterLevel; y++) {
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ), Blocks.WATER.getDefaultState(), false);
                    }
                    // 湖底是淤泥/冰混合物
                    for (int y = height - 2; y < height; y++) {
                        chunk.setBlockState(new BlockPos(worldX, y, worldZ),
                                noise(worldX, y, worldZ) > 0 ? Blocks.CLAY.getDefaultState() : Blocks.PACKED_ICE.getDefaultState(), false);
                    }
                } else {
                    // 陆地冰冠
                    // 底层：蓝冰（压实冰）
                    chunk.setBlockState(new BlockPos(worldX, height - 2, worldZ), Blocks.BLUE_ICE.getDefaultState(), false);
                    // 中层：浮冰
                    chunk.setBlockState(new BlockPos(worldX, height - 1, worldZ), Blocks.PACKED_ICE.getDefaultState(), false);
                    // 表层：雪/冰 depending on temperature noise
                    double tempNoise = noise(worldX * 0.05, worldZ * 0.05);
                    if (tempNoise > 0.3 && height > 70) {
                        // 高海拔积雪
                        chunk.setBlockState(new BlockPos(worldX, height, worldZ), Blocks.SNOW_BLOCK.getDefaultState(), false);
                        // 雪层
                        if (height < 88) {
                            chunk.setBlockState(new BlockPos(worldX, height + 1, worldZ), Blocks.SNOW.getDefaultState(), false);
                        }
                    } else {
                        // 普通冰面
                        chunk.setBlockState(new BlockPos(worldX, height, worldZ), Blocks.ICE.getDefaultState(), false);
                    }

                    // 偶尔暴露的岩石（冰火山？）
                    if (detailNoise > 0.85) {
                        chunk.setBlockState(new BlockPos(worldX, height, worldZ), Blocks.STONE.getDefaultState(), false);
                        chunk.setBlockState(new BlockPos(worldX, height + 1, worldZ), Blocks.COBBLESTONE.getDefaultState(), false);
                    }
                }

                // 填充高度以下的空气（防止浮空）
                for (int y = height + 1; y < getWorldHeight(); y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (chunk.getBlockState(pos).isAir() && y <= waterLevel && isLake) {
                        // 湖面上方的空气不填充，但湖面以下已经处理
                    }
                }
            }
        }
    }

    // === 噪声函数 ===

    /** 基础地形噪声 - 大尺度起伏 */
    private double sampleBaseNoise(int x, int z) {
        return sinNoise(x * 0.02, z * 0.02) * 0.6
                + sinNoise(x * 0.05 + 100, z * 0.03 + 50) * 0.4;
    }

    /** 细节噪声 - 小尺度变化 */
    private double sampleDetailNoise(int x, int z) {
        return sinNoise(x * 0.1, z * 0.1) * 0.5
                + sinNoise(x * 0.2 + 33, z * 0.15 + 77) * 0.3
                + sinNoise(x * 0.4, z * 0.4) * 0.2;
    }

    /** 湖泊噪声 - 决定低洼处是否积水 */
    private double sampleLakeNoise(int x, int z) {
        return sinNoise(x * 0.015 + 500, z * 0.015 + 500) * 0.7
                + sinNoise(x * 0.03, z * 0.03) * 0.3;
    }

    /** 简单可重复噪声（基于 sin，不需要 Perlin/Simplex） */
    private double sinNoise(double x, double z) {
        return Math.sin(x) * Math.cos(z);
    }

    private double noise(double x, double y, double z) {
        return Math.sin(x * 12.9898 + y * 78.233 + z * 37.719) * 43758.5453 % 1.0;
    }

    private double noise(double x, double z) {
        return Math.sin(x * 12.9898 + z * 78.233) * 43758.5453 % 1.0;
    }
}