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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class EyeOfHarmonyObeliskBlockEntity extends BlockEntity implements IFluidSource, ArtronHolder {

    private float yOffset = 0.0f;
    private float scale = 1.0f;
    private boolean active = true;
    private boolean eyeVisible = true;
    private boolean redstoneMode = false;

    public static final int CHARGE_RADIUS = 3;
    public static final double CHARGE_PER_TICK = 5.0;
    private int tickCounter = 0;

    public EyeOfHarmonyObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_OBELISK, pos, state);
    }

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
        boolean wasActive = this.active;
        this.active = active;
        markDirty();
        sync();

        // 激活/关闭时播放一次性音效
        if (world != null && !world.isClient) {
            if (!wasActive && active) {
                world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE,
                        SoundCategory.BLOCKS, 1.0f, 0.8f);
            } else if (wasActive && !active) {
                world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE,
                        SoundCategory.BLOCKS, 1.0f, 0.8f);
            }
        }
    }

    public boolean isEyeVisible() { return eyeVisible; }
    public void setEyeVisible(boolean visible) {
        this.eyeVisible = visible;
        markDirty();
        sync();
    }

    public boolean isRedstoneMode() { return redstoneMode; }

    public void setRedstoneMode(boolean redstoneMode) {
        this.redstoneMode = redstoneMode;
        if (world != null && !world.isClient) {
            if (redstoneMode) {
                updateRedstoneState();
            } else {
                setActive(true);
            }
        }
        markDirty();
        sync();
    }

    public void updateRedstoneState() {
        if (!redstoneMode || world == null || world.isClient) return;
        int power = getMaxRedstonePower();
        boolean shouldBeActive = power > 0;
        if (active != shouldBeActive) {
            setActive(shouldBeActive);
        }
    }

    private int getMaxRedstonePower() {
        int p1 = world.getReceivedRedstonePower(pos);
        int p2 = world.getReceivedRedstonePower(pos.up());
        int p3 = world.getReceivedRedstonePower(pos.down());
        return Math.max(p1, Math.max(p2, p3));
    }

    public void tick() {
        if (world == null || world.isClient) return;

        if (redstoneMode) {
            updateRedstoneState();
        }

        // 工作中定期播放信标环境音
        if (active && world.getTime() % 70 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_AMBIENT,
                    SoundCategory.BLOCKS, 0.6f, 0.9f);
        }

        if (!active) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return;

        ServerWorld serverWorld = (ServerWorld) world;
        RiftChunkManager manager = RiftChunkManager.getInstance(serverWorld);
        ChunkPos center = new ChunkPos(pos);

        for (int dx = -CHARGE_RADIUS; dx <= CHARGE_RADIUS; dx++) {
            for (int dz = -CHARGE_RADIUS; dz <= CHARGE_RADIUS; dz++) {
                ChunkPos target = new ChunkPos(center.x + dx, center.z + dz);
                if (manager.isRiftChunk(target)) {
                    manager.addFuel(target, CHARGE_PER_TICK * 20);
                }
            }
        }
    }

    // IFluidSource / ArtronHolder
    @Override public double level() { return Double.MAX_VALUE / 2; }
    @Override public void setLevel(double level) {}
    @Override public double maxLevel() { return Double.MAX_VALUE / 2; }
    @Override public void onChange(double before, double after) {}
    @Override public IFluidSource source(boolean search) { return this; }
    @Override public void setSource(IFluidSource source) {}
    @Override public IFluidLink last() { return this; }
    @Override public void setLast(IFluidLink last) {}
    @Override public double getCurrentFuel() { return Double.MAX_VALUE / 2; }
    @Override public void setCurrentFuel(double var) {}
    @Override public double getMaxFuel() { return Double.MAX_VALUE / 2; }
    @Override public void removeFuel(double var) {}
    @Override public double addFuel(double var) { return 0; }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.yOffset = nbt.getFloat("YOffset");
        this.scale = nbt.getFloat("Scale");
        if (this.scale == 0.0f) this.scale = 1.0f;
        this.active = nbt.getBoolean("Active");
        this.eyeVisible = nbt.getBoolean("EyeVisible");
        this.redstoneMode = nbt.getBoolean("RedstoneMode");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("YOffset", yOffset);
        nbt.putFloat("Scale", scale);
        nbt.putBoolean("Active", active);
        nbt.putBoolean("EyeVisible", eyeVisible);
        nbt.putBoolean("RedstoneMode", redstoneMode);
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