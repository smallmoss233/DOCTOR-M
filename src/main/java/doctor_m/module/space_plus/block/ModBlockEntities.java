package doctor_m.module.space_plus.block;

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

    public static void register() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("doctor_m", "oxygen_charger_entity"), OXYGEN_CHARGER_ENTITY);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("doctor_m", "underwater_oxygen_generator_entity"), UNDERWATER_OXYGEN_GENERATOR_ENTITY);
    }
}