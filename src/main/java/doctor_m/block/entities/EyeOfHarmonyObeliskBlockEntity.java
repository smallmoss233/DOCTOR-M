package doctor_m.block.entities;

import dev.amble.ait.api.ArtronHolder;
import dev.amble.ait.core.engine.link.IFluidLink;
import dev.amble.ait.core.engine.link.IFluidSource;
import dev.amble.ait.core.world.RiftChunkManager;
import doctor_m.block.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class EyeOfHarmonyObeliskBlockEntity extends BlockEntity implements IFluidSource, ArtronHolder {

    private float yOffset = 0.0f;
    private float scale = 1.0f;
    private boolean active = true;

    // 方尖碑参数
    public static final int CHARGE_RADIUS = 3;          // 影响半径（区块数）
    public static final double CHARGE_PER_TICK = 5.0;   // 每 tick 给单个 rift chunk 充能
    private int tickCounter = 0;

    public EyeOfHarmonyObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_OBELISK, pos, state);
    }

    // ========== 原有 getter/setter ==========
    public float getYOffset() { return yOffset; }
    public void setYOffset(float yOffset) {
        this.yOffset = yOffset;
        markDirty();
        sync();
    }
    public float getScale() { return scale; }
    public void setScale(float scale) {
        this.scale = scale;
        markDirty();
        sync();
    }
    public boolean isActive() { return active; }
    public void setActive(boolean active) {
        this.active = active;
        markDirty();
        sync();
    }

    // ========== 新增：每 tick 给周围 Rift Chunk 充能 ==========
    public void tick() {
        if (world == null || world.isClient) return;
        if (!active) return;

        tickCounter++;
        // 每 20 tick（1秒）充能一次，降低性能开销
        if (tickCounter % 20 != 0) return;

        ServerWorld serverWorld = (ServerWorld) world;
        RiftChunkManager manager = RiftChunkManager.getInstance(serverWorld);
        ChunkPos center = new ChunkPos(pos);

        for (int dx = -CHARGE_RADIUS; dx <= CHARGE_RADIUS; dx++) {
            for (int dz = -CHARGE_RADIUS; dz <= CHARGE_RADIUS; dz++) {
                ChunkPos target = new ChunkPos(center.x + dx, center.z + dz);
                if (manager.isRiftChunk(target)) {
                    manager.addFuel(target, CHARGE_PER_TICK * 20); // 一次给 20 tick 的量
                }
            }
        }
    }

    // ========== IFluidSource 实现（无限 AU 源） ==========

    @Override
    public double level() {
        return Double.MAX_VALUE / 2;
    }

    @Override
    public void setLevel(double level) {
        // 空操作：无限能源，无视任何扣减/增加
    }

    @Override
    public double maxLevel() {
        return Double.MAX_VALUE / 2;
    }

    // ========== IFluidLink 实现 ==========

    @Override
    public IFluidSource source(boolean search) {
        return this; // 自己就是源
    }

    @Override
    public void setSource(IFluidSource source) {
        // 源不需要被设置源
    }

    @Override
    public IFluidLink last() {
        return this;
    }

    @Override
    public void setLast(IFluidLink last) {
        // 源不需要上游
    }

    // ========== ArtronHolder 实现（兼容 AIT 的 Artron 接口） ==========

    @Override
    public double getCurrentFuel() {
        return Double.MAX_VALUE / 2;
    }

    @Override
    public void setCurrentFuel(double var) {
        // 空操作
    }

    @Override
    public double getMaxFuel() {
        return Double.MAX_VALUE / 2;
    }

    @Override
    public void removeFuel(double var) {
        // 无限能源，扣多少都无所谓
    }

    @Override
    public double addFuel(double var) {
        // 来者不拒，但返回溢出量（永远为0）
        return 0;
    }

    // ========== NBT / 网络同步 ==========

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.yOffset = nbt.getFloat("YOffset");
        this.scale = nbt.getFloat("Scale");
        if (this.scale == 0.0f) this.scale = 1.0f;
        this.active = nbt.getBoolean("Active");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("YOffset", yOffset);
        nbt.putFloat("Scale", scale);
        nbt.putBoolean("Active", active);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    private void sync() {
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }
}