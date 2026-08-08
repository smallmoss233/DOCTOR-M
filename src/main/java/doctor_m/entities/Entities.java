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
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
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
            // 只在雪地 biome 注册（主世界雪地 + 特兰泽洛全境）
            var snowyBiomes = BiomeSelectors.includeByKey(
                    BiomeKeys.SNOWY_PLAINS,
                    BiomeKeys.SNOWY_SLOPES,
                    BiomeKeys.SNOWY_TAIGA,
                    BiomeKeys.ICE_SPIKES,
                    BiomeKeys.FROZEN_PEAKS
            );

            // 基础权重都低，成群1只
            BiomeModifications.addSpawn(snowyBiomes, SpawnGroup.CREATURE, Entities.TYPE_103_TARDIS, 2, 1, 1);
            BiomeModifications.addSpawn(snowyBiomes, SpawnGroup.CREATURE, Entities.MARIAN_JIN, 2, 1, 1);

            // 103型：主世界极稀有，特兰泽洛中等
            SpawnRestriction.register(TYPE_103_TARDIS,
                    SpawnRestriction.Location.ON_GROUND,
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    (type, world, reason, pos, random) -> {
                        RegistryKey<World> dim = world.toServerWorld().getRegistryKey();
                        if (dim == World.OVERWORLD) return random.nextFloat() < 0.08f;
                        if (dim.equals(TRENZALORE_DIM)) return random.nextFloat() < 0.4f;
                        return false;
                    }
            );

            // 玛丽安：主世界同103，特兰泽洛概率更高
            SpawnRestriction.register(MARIAN_JIN,
                    SpawnRestriction.Location.ON_GROUND,
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    (type, world, reason, pos, random) -> {
                        RegistryKey<World> dim = world.toServerWorld().getRegistryKey();
                        if (dim == World.OVERWORLD) return random.nextFloat() < 0.08f;
                        if (dim.equals(TRENZALORE_DIM)) return random.nextFloat() < 0.65f;
                        return false;
                    }
            );
    }
}