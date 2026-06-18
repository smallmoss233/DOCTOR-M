package doctor_m.module.space;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<OxygenChargerBlockEntity> OXYGEN_CHARGER_ENTITY =
            FabricBlockEntityTypeBuilder.create(OxygenChargerBlockEntity::new, ModBlocks.OXYGEN_CHARGER).build();

    public static void register() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("doctor_m", "oxygen_charger_entity"), OXYGEN_CHARGER_ENTITY);
    }
}