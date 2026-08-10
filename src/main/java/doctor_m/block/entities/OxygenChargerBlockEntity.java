package doctor_m.block.entities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import static doctor_m.block.ModBlockEntities.OXYGEN_CHARGER_ENTITY;

public class OxygenChargerBlockEntity extends BlockEntity {

    private long cooldownEndTick = 0; // 冷却结束时的游戏 tick

    public OxygenChargerBlockEntity(BlockPos pos, BlockState state) {
        super(OXYGEN_CHARGER_ENTITY, pos, state);
    }

    public long getCooldownEndTick() {
        return cooldownEndTick;
    }

    public void setCooldownEndTick(long cooldownEndTick) {
        this.cooldownEndTick = cooldownEndTick;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.cooldownEndTick = nbt.getLong("CooldownEndTick");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("CooldownEndTick", this.cooldownEndTick);
    }
}