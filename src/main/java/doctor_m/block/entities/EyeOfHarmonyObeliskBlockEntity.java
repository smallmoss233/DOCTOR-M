package doctor_m.block.entities;

import doctor_m.block.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

/**
 * 和谐之眼方尖碑的方块实体
 * 负责存储渲染 Y 轴偏移量
 */
public class EyeOfHarmonyObeliskBlockEntity extends BlockEntity {

    private float yOffset = 10.0f;

    public EyeOfHarmonyObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_OBELISK, pos, state);
    }

    public float getYOffset() {
        return yOffset;
    }

    public void setYOffset(float yOffset) {
        this.yOffset = yOffset;
        markDirty();
        // 同步到客户端
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.yOffset = nbt.getFloat("YOffset");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("YOffset", yOffset);
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