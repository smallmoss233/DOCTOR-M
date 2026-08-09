package doctor_m.entities;

import doctor_m.DOCTORM;
import doctor_m.entities.data.Entity103Tardis;
import doctor_m.entities.data.Marian_Jin;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class Entities {

    public static final EntityType<Entity103Tardis> TYPE_103_TARDIS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "type_103_tardis"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, Entity103Tardis::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build()
    );

    public static final EntityType<Marian_Jin> MARIAN_JIN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "marian_jin"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, Marian_Jin::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build()
    );

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(TYPE_103_TARDIS, Entity103Tardis.createMobAttributes());
        FabricDefaultAttributeRegistry.register(MARIAN_JIN, Marian_Jin.createMobAttributes());
    }
        private static final RegistryKey<World> TRENZALORE_DIM = RegistryKey.of(
                RegistryKeys.WORLD, new Identifier("doctor_m", "trenzalore")
        );

        public static void registerSpawns() {
        // 103型：主世界所有群系都注册（SpawnRestriction 负责过滤海洋）
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE, Entities.TYPE_103_TARDIS, 2, 1, 1
        );

        // 玛丽安：同上，全主世界注册，实际概率由 SpawnRestriction 控制
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE, Entities.MARIAN_JIN, 2, 1, 1
        );

        // 103型生成限制：不在海洋生成
        SpawnRestriction.register(TYPE_103_TARDIS,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> {
                    if (isOcean(world.getBiome(pos))) return false;

                    RegistryKey<World> dim = world.toServerWorld().getRegistryKey();
                    if (dim == World.OVERWORLD) return random.nextFloat() < 0.08f;
                    if (dim.equals(TRENZALORE_DIM)) return random.nextFloat() < 0.2f;
                    return false;
                }
        );

        // 玛丽安生成限制：雪原正常概率，其他陆地极低，海洋不生成
        SpawnRestriction.register(MARIAN_JIN,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> {
                    var biome = world.getBiome(pos);
                    if (isOcean(biome)) return false;

                    RegistryKey<World> dim = world.toServerWorld().getRegistryKey();
                    if (dim == World.OVERWORLD) {
                        if (isSnowy(biome)) return random.nextFloat() < 0.08f;
                        return random.nextFloat() < 0.02f; // 其他陆地群系极低
                    }
                    if (dim.equals(TRENZALORE_DIM)) return random.nextFloat() < 0.3f;
                    return false;
                }
        );
    }

    private static boolean isOcean(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_OCEAN)
                || biome.matchesKey(BiomeKeys.WARM_OCEAN)
                || biome.matchesKey(BiomeKeys.LUKEWARM_OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_LUKEWARM_OCEAN)
                || biome.matchesKey(BiomeKeys.COLD_OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_COLD_OCEAN)
                || biome.matchesKey(BiomeKeys.FROZEN_OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_FROZEN_OCEAN);
    }

    private static boolean isSnowy(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.SNOWY_PLAINS)
                || biome.matchesKey(BiomeKeys.SNOWY_TAIGA)
                || biome.matchesKey(BiomeKeys.SNOWY_SLOPES)
                || biome.matchesKey(BiomeKeys.ICE_SPIKES)
                || biome.matchesKey(BiomeKeys.FROZEN_PEAKS)
                || biome.matchesKey(BiomeKeys.GROVE);
    }
}