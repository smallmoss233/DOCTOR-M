package doctor_m.module.space_plus.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

import static doctor_m.module.space_plus.block.ModBlockEntities.UNDERWATER_OXYGEN_GENERATOR_ENTITY;

public class UnderwaterOxygenGeneratorBlockEntity extends BlockEntity {

    private static final double RANGE = 5.0;
    private static final int EFFECT_DURATION = 80; // 4 秒，每 tick 刷新

    private long cooldownEndTick = 0;
    private int waterCharges = 0;
    private static final int MAX_WATER_CHARGES = 16;

    public UnderwaterOxygenGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(UNDERWATER_OXYGEN_GENERATOR_ENTITY, pos, state);
    }

    public long getCooldownEndTick() {
        return cooldownEndTick;
    }

    public void setCooldownEndTick(long cooldownEndTick) {
        this.cooldownEndTick = cooldownEndTick;
    }

    public int getWaterCharges() {
        return waterCharges;
    }

    public int addWaterCharges(int amount) {
        int old = waterCharges;
        waterCharges = Math.min(waterCharges + amount, MAX_WATER_CHARGES);
        int added = waterCharges - old;
        if (added > 0) markDirty();
        return added;
    }

    public void consumeWaterCharge() {
        if (waterCharges > 0) {
            waterCharges--;
            markDirty();
        }
    }

    /** 检测是否处于水下环境：周围 6 个面至少有一个是水 */
    private static boolean isUnderwaterEnvironment(World world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (world.getFluidState(neighbor).isIn(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** BlockEntityTicker 回调 */
    public static void tick(World world, BlockPos pos, BlockState state, UnderwaterOxygenGeneratorBlockEntity be) {
        if (world.isClient()) return;

        if (isUnderwaterEnvironment(world, pos)) {
            Box box = new Box(pos).expand(RANGE);
            List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, box, player -> true);
            for (PlayerEntity player : players) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WATER_BREATHING,
                        EFFECT_DURATION,
                        0,
                        false,
                        false,
                        true
                ));
            }
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.cooldownEndTick = nbt.getLong("CooldownEndTick");
        this.waterCharges = nbt.getInt("WaterCharges");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("CooldownEndTick", this.cooldownEndTick);
        nbt.putInt("WaterCharges", this.waterCharges);
    }
}