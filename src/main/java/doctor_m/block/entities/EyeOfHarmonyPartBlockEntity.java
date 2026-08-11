package doctor_m.block.entities;

import dev.amble.ait.core.engine.link.IFluidLink;
import dev.amble.ait.core.engine.link.IFluidSource;
import doctor_m.block.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

public class EyeOfHarmonyPartBlockEntity extends BlockEntity implements IFluidLink {

    public EyeOfHarmonyPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_PART, pos, state);
    }

    /** 查找相邻的主方块 BE */
    private EyeOfHarmonyObeliskBlockEntity findMainObelisk() {
        if (world == null) return null;

        BlockPos down = pos.down();
        BlockEntity be = world.getBlockEntity(down);
        if (be instanceof EyeOfHarmonyObeliskBlockEntity obelisk) {
            return obelisk;
        }

        BlockPos up = pos.up();
        be = world.getBlockEntity(up);
        if (be instanceof EyeOfHarmonyObeliskBlockEntity obelisk) {
            return obelisk;
        }

        return null;
    }

    // ========== IFluidLink 实现：全部透传给主方块 ==========

    @Override
    public IFluidSource source(boolean search) {
        EyeOfHarmonyObeliskBlockEntity main = findMainObelisk();
        return main != null ? main : null;
    }

    @Override
    public void setSource(IFluidSource source) {
        // 辅助方块不存储网络状态，由主方块承担 source 角色
    }

    @Override
    public IFluidLink last() {
        // 上游就是主方块
        return findMainObelisk();
    }

    @Override
    public void setLast(IFluidLink last) {
        // 辅助方块不存储网络状态
    }

    // ========== NBT / 网络同步（辅助方块无持久化数据，但保留接口） ==========

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}