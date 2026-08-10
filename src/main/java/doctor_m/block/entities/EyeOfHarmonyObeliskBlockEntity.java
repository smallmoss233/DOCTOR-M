package doctor_m.block.entities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

import doctor_m.block.ModBlockEntities;

public class EyeOfHarmonyObeliskBlockEntity extends BlockEntity {

    private float yOffset = 5.0f;
    private float scale = 1.0f;  // ← 默认 1.0 = 原大小

    public EyeOfHarmonyObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_OBELISK, pos, state);
    }

    public float getYOffset() {
        return yOffset;
    }

    public void setYOffset(float yOffset) {
        this.yOffset = yOffset;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.yOffset = nbt.getFloat("YOffset");
        this.scale = nbt.getFloat("Scale");
        if (this.scale == 0.0f) this.scale = 1.0f; // 兼容旧存档
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("YOffset", yOffset);
        nbt.putFloat("Scale", scale);
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