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
import net.minecraft.util.math.Direction;

public class EyeOfHarmonyPartBlockEntity extends BlockEntity implements IFluidLink {

    public EyeOfHarmonyPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_PART, pos, state);
    }

    // ========== 查找相邻的 IFluidSource（不包括主方块） ==========
    private IFluidSource findConnectedSource() {
        if (world == null) return null;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            BlockEntity be = world.getBlockEntity(neighborPos);

            // 如果邻居是主方块（方尖碑），跳过（能量应该往外流，不回流）
            if (be instanceof EyeOfHarmonyObeliskBlockEntity) continue;

            // 如果邻居是 IFluidSource，返回它
            if (be instanceof IFluidSource source) {
                return source;
            }

            // 如果邻居是 IFluidLink（线缆或其他辅助方块），继续询问它
            if (be instanceof IFluidLink link) {
                IFluidSource deeper = link.source(true);
                if (deeper != null) return deeper;
            }
        }
        return null;
    }

    // ========== IFluidLink 实现 ==========

    @Override
    public IFluidSource source(boolean search) {
        if (!search) return findConnectedSource();
        // 如果 search 为 true，递归查找最终目标
        return findConnectedSource();
    }

    @Override
    public void setSource(IFluidSource source) {
        // 辅助方块不存储网络状态
    }

    @Override
    public IFluidLink last() {
        return null; // 辅助方块作为中间节点，不保存上游
    }

    @Override
    public void setLast(IFluidLink last) {
        // 辅助方块不存储网络状态
    }

    // ========== NBT / 网络同步 ==========
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