package doctor_m.entities;

import doctor_m.DOCTORM;
import doctor_m.api.AutoRegister;
import doctor_m.entities.data.Entity103Tardis;
import doctor_m.entities.data.Marian_Jin;
import doctor_m.module.dalek.DalekEntity;
import doctor_m.module.dalek.DalekRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class Entities {

    public static final EntityType<Entity103Tardis> TYPE_103_TARDIS =
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, Entity103Tardis::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build();

    public static final EntityType<Marian_Jin> MARIAN_JIN =
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, Marian_Jin::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build();

    public static final EntityType<DalekEntity> DALEK =
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build();

    private static final RegistryKey<World> TRENZALORE_DIM = RegistryKey.of(
            RegistryKeys.WORLD, new Identifier("doctor_m", "trenzalore")
    );

    /** 统一注册入口：实体类型 → 属性 → 生成 */
    public static void register() {
        AutoRegister.entities(Entities.class, DOCTORM.MOD_ID);
        DalekRegistry.getInstance().onCommonInit();
        registerAttributes();
        registerSpawns();
    }

    private static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(TYPE_103_TARDIS, Entity103Tardis.createMobAttributes());
        FabricDefaultAttributeRegistry.register(MARIAN_JIN, Marian_Jin.createMobAttributes());
        FabricDefaultAttributeRegistry.register(DALEK, DalekEntity.createDalekAttributes());
    }

    private static void registerSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE, TYPE_103_TARDIS, 2, 1, 1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.CREATURE, MARIAN_JIN, 2, 1, 1
        );
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                SpawnGroup.MONSTER, DALEK, 1, 1, 1
        );

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

        SpawnRestriction.register(MARIAN_JIN,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> {
                    var biome = world.getBiome(pos);
                    if (isOcean(biome)) return false;

                    RegistryKey<World> dim = world.toServerWorld().getRegistryKey();
                    if (dim == World.OVERWORLD) {
                        if (isSnowy(biome)) return random.nextFloat() < 0.08f;
                        return random.nextFloat() < 0.02f;
                    }
                    if (dim.equals(TRENZALORE_DIM)) return random.nextFloat() < 0.3f;
                    return false;
                }
        );

        SpawnRestriction.register(DALEK,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> {
                    if (isOcean(world.getBiome(pos))) return false;

                    RegistryKey<World> dim = world.toServerWorld().getRegistryKey();
                    if (dim == World.OVERWORLD) return random.nextFloat() < 0.05f;
                    if (dim.equals(TRENZALORE_DIM)) return random.nextFloat() < 0.15f;
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