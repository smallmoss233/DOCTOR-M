package doctor_m.block.entity;

import doctor_m.CoffeeMachine.CoffeeMachineBlockEntity;
import doctor_m.DOCTORM;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DOCTORMBlockEntities {
    public static BlockEntityType<CoffeeMachineBlockEntity> COFFEE_MACHINE_BLOCK_ENTITY;

    public static void registerBlockEntities() {
        COFFEE_MACHINE_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(DOCTORM.MOD_ID, "coffee_machine_block_entity"),
                BlockEntityType.Builder.create(
                        CoffeeMachineBlockEntity::new,
                        DOCTORM.COFFEE_MACHINE_BLOCK
                ).build(null)
        );
    }
}