package doctor_m.block;

import doctor_m.DOCTORM;
import doctor_m.api.AutoRegister;
import doctor_m.block.entities.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static final BlockEntityType<OxygenChargerBlockEntity> OXYGEN_CHARGER_ENTITY =
            FabricBlockEntityTypeBuilder.create(OxygenChargerBlockEntity::new, ModBlocks.OXYGEN_CHARGER).build();

    public static final BlockEntityType<UnderwaterOxygenGeneratorBlockEntity> UNDERWATER_OXYGEN_GENERATOR_ENTITY =
            FabricBlockEntityTypeBuilder.create(UnderwaterOxygenGeneratorBlockEntity::new, ModBlocks.UNDERWATER_OXYGEN_GENERATOR).build();

    public static final BlockEntityType<EyeOfHarmonyObeliskBlockEntity> EYE_OF_HARMONY_OBELISK =
            FabricBlockEntityTypeBuilder.create(EyeOfHarmonyObeliskBlockEntity::new, ModBlocks.EYE_OF_HARMONY_OBELISK).build();

    public static final BlockEntityType<EyeOfHarmonyPartBlockEntity> EYE_OF_HARMONY_PART =
            FabricBlockEntityTypeBuilder.create(EyeOfHarmonyPartBlockEntity::new, ModBlocks.EYE_OF_HARMONY_PART).build();

    public static final BlockEntityType<ToyotaSpinningRotorBlockEntity> TOYOTA_SPINNING_ROTOR =
            FabricBlockEntityTypeBuilder.create(ToyotaSpinningRotorBlockEntity::new, ModBlocks.TOYOTA_SPINNING_ROTOR).build();

    public static final BlockEntityType<CoffeeMachineBlockEntity> COFFEE_MACHINE =
            FabricBlockEntityTypeBuilder.create(CoffeeMachineBlockEntity::new, ModBlocks.COFFEE_MACHINE).build();

    public static void register() {
        AutoRegister.blockEntities(ModBlockEntities.class, DOCTORM.MOD_ID);
    }
}