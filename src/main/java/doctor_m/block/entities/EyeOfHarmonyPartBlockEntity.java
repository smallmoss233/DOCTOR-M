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

import java.util.HashSet;
import java.util.Set;

public class EyeOfHarmonyPartBlockEntity extends BlockEntity implements IFluidLink {

    public EyeOfHarmonyPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_PART, pos, state);
    }

    // ========== 查找相邻的 IFluidSource（不包括主方块，带循环保护） ==========
    private IFluidSource findConnectedSource(Set<BlockPos> visited) {
        if (world == null) return null;
        if (visited == null) visited = new HashSet<>();

        // 防止循环：如果当前节点已经被访问过，立即返回 null
        if (visited.contains(pos)) return null;
        visited.add(pos);

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);
            if (visited.contains(neighborPos)) continue;

            BlockEntity be = world.getBlockEntity(neighborPos);

            // 如果邻居是主方块（方尖碑），跳过
            if (be instanceof EyeOfHarmonyObeliskBlockEntity) continue;

            // 如果邻居是 IFluidSource，返回它
            if (be instanceof IFluidSource source) {
                return source;
            }

            // 如果邻居是 IFluidLink（线缆或其他辅助方块），递归询问它
            if (be instanceof IFluidLink link) {
                // 如果 link 是另一个辅助方块，递归调用（传递 visited）
                if (link instanceof EyeOfHarmonyPartBlockEntity partBE) {
                    IFluidSource deeper = partBE.findConnectedSource(visited);
                    if (deeper != null) return deeper;
                } else {
                    // 普通 IFluidLink（如线缆），调用其 source()
                    IFluidSource deeper = link.source(true);
                    if (deeper != null) return deeper;
                }
            }
        }
        return null;
    }

    // 无参入口方法（自动创建 visited 集合）
    private IFluidSource findConnectedSource() {
        return findConnectedSource(new HashSet<>());
    }

    // ========== IFluidLink 实现 ==========

    @Override
    public IFluidSource source(boolean search) {
        if (!search) return null;
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