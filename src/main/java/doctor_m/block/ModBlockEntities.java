package doctor_m.block;

import doctor_m.DOCTORM;
import doctor_m.block.entities.*;
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

    public static final BlockEntityType<ToyotaSpinningRotorBlockEntity> TOYOTA_SPINNING_ROTOR =
            FabricBlockEntityTypeBuilder.create(ToyotaSpinningRotorBlockEntity::new, ModBlocks.TOYOTA_SPINNING_ROTOR).build();

    public static final BlockEntityType<CoffeeMachineBlockEntity> COFFEE_MACHINE =
            FabricBlockEntityTypeBuilder.create(CoffeeMachineBlockEntity::new, ModBlocks.COFFEE_MACHINE).build();

    public static void register() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DOCTORM.MOD_ID, "oxygen_charger_entity"), OXYGEN_CHARGER_ENTITY);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(DOCTORM.MOD_ID, "underwater_oxygen_generator_entity"), UNDERWATER_OXYGEN_GENERATOR_ENTITY);

        EYE_OF_HARMONY_OBELISK = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(DOCTORM.MOD_ID, "eye_of_harmony_obelisk"),
                FabricBlockEntityTypeBuilder.create(
                        EyeOfHarmonyObeliskBlockEntity::new,
                        ModBlocks.EYE_OF_HARMONY_OBELISK
                ).build());

        Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(DOCTORM.MOD_ID, "coffee_machine_block_entity"),
                COFFEE_MACHINE
        );

        Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(DOCTORM.MOD_ID, "toyota_spinning_rotor"),
                TOYOTA_SPINNING_ROTOR
        );
    }
}