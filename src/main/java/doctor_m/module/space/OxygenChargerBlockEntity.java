package doctor_m.module.space;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class OxygenChargerBlockEntity extends BlockEntity {

    public OxygenChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_CHARGER_ENTITY, pos, state);
    }
}