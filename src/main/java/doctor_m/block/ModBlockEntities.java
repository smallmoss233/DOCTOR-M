package doctor_m.block;

import doctor_m.DOCTORM;
import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import doctor_m.block.entities.EyeOfHarmonyPartBlockEntity;
import doctor_m.block.entities.OxygenChargerBlockEntity;
import doctor_m.block.entities.UnderwaterOxygenGeneratorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<OxygenChargerBlockEntity> OXYGEN_CHARGER_ENTITY =
            FabricBlockEntityTypeBuilder.create(OxygenChargerBlockEntity::new, ModBlocks.OXYGEN_CHARGER).build();

    public static final BlockEntityType<UnderwaterOxygenGeneratorBlockEntity> UNDERWATER_OXYGEN_GENERATOR_ENTITY =
            FabricBlockEntityTypeBuilder.create(UnderwaterOxygenGeneratorBlockEntity::new, ModBlocks.UNDERWATER_OXYGEN_GENERATOR).build();

    public static BlockEntityType<EyeOfHarmonyObeliskBlockEntity> EYE_OF_HARMONY_OBELISK;

    public static final BlockEntityType<EyeOfHarmonyPartBlockEntity> EYE_OF_HARMONY_PART =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    new Identifier(DOCTORM.MOD_ID, "eye_of_harmony_part"),
                    FabricBlockEntityTypeBuilder.create(EyeOfHarmonyPartBlockEntity::new, ModBlocks.EYE_OF_HARMONY_PART).build()
            );

    public static void register() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("doctor_m", "oxygen_charger_entity"), OXYGEN_CHARGER_ENTITY);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("doctor_m", "underwater_oxygen_generator_entity"), UNDERWATER_OXYGEN_GENERATOR_ENTITY);
        EYE_OF_HARMONY_OBELISK = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier("doctor_m", "eye_of_harmony_obelisk"),
                FabricBlockEntityTypeBuilder.create(
                        EyeOfHarmonyObeliskBlockEntity::new,
                        ModBlocks.EYE_OF_HARMONY_OBELISK
                ).build());
    }
}