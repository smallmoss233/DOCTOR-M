package doctor_m.block.entities;

import dev.amble.ait.api.ArtronHolder;
import dev.amble.ait.core.engine.link.IFluidLink;
import dev.amble.ait.core.engine.link.IFluidSource;
import doctor_m.block.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class EyeOfHarmonyObeliskBlockEntity extends BlockEntity implements IFluidSource, ArtronHolder {

    // ====== 渲染参数 ======
    private float yOffset = 0.0f;
    private float scale = 1.0f;
    private boolean active = true;
    private boolean eyeVisible = true;
    private boolean redstoneMode = false;

    // ====== 能量存储 ======
    private double artronAmount = 0.0;
    public static final double GENERATION_RATE = 20;      // 每 tick 产生量
    public static final double MAX_STORAGE = 100000.0;      // 最大存储
    public static final double TRANSFER_RATE = 200;       // 每次传输上限
    private int generationCounter = 0;

    public EyeOfHarmonyObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EYE_OF_HARMONY_OBELISK, pos, state);
    }

    // ====== getter/setter（原有方法保持不变） ======
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

    // ====== 供 GUI 调用的方法 ======
    public double getEnergyPercentage() {
        return artronAmount / MAX_STORAGE;
    }

    // ====== 核心 tick ======
    public void tick() {
        if (world == null) return;

        if (world.isClient) {
            return;
        }

        // 服务端逻辑
        if (redstoneMode) {
            updateRedstoneState();
        }
        if (!active) return;

        // 产生能量（每 tick 产生，每 20 tick 结算）
        generationCounter++;
        if (generationCounter >= 20) {
            generationCounter = 0;
            produceAndDistribute();
        }
    }

    // ====== 产生并分发 ======
    private void produceAndDistribute() {
        double produced = GENERATION_RATE * 20;
        artronAmount = Math.min(artronAmount + produced, MAX_STORAGE);
        markDirty();
        sync();

        if (artronAmount > 0) {
            distributeToNetwork();
        }
    }

    //阿特隆能量传输
    private void distributeToNetwork() {
        if (world == null || world.isClient) return;
        if (artronAmount <= 0) return;

        // BFS 遍历所有连接的 IFluidLink
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(pos);
        visited.add(pos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockEntity currentBE = world.getBlockEntity(current);

            // 如果当前节点是 IFluidLink，尝试向它的 source() 传输
            if (currentBE instanceof IFluidLink link) {
                IFluidSource target = link.source(true);
                if (target != null && target != this && !isTargetFull(target)) {
                    transferToTarget(target);
                    if (artronAmount <= 0) return;
                }
            }

            // 继续遍历邻居（辅助方块和线缆）
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (visited.contains(neighbor)) continue;

                BlockEntity neighborBE = world.getBlockEntity(neighbor);
                // 只有 IFluidLink 才继续遍历
                if (neighborBE instanceof IFluidLink) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
    }

    private void transferToTarget(IFluidSource target) {
        if (target == null || target == this || artronAmount <= 0) return;

        double transferAmount = Math.min(artronAmount, TRANSFER_RATE);
        double actualTransfer = 0;

        if (target instanceof ArtronHolder holder) {
            double space = holder.getMaxFuel() - holder.getCurrentFuel();
            actualTransfer = Math.min(transferAmount, space);
            if (actualTransfer > 0) {
                holder.addFuel(actualTransfer);
            }
        } else {
            double space = target.maxLevel() - target.level();
            actualTransfer = Math.min(transferAmount, space);
            if (actualTransfer > 0) {
                target.addLevel(actualTransfer);
            }
        }

        if (actualTransfer > 0) {
            this.removeFuel(actualTransfer);
            markDirty();
            sync();
        }
    }

    private boolean isTargetFull(IFluidSource target) {
        if (target instanceof ArtronHolder holder) {
            return holder.getCurrentFuel() >= holder.getMaxFuel();
        }
        return target.level() >= target.maxLevel();
    }

    // ====== IFluidSource 实现 ======
    @Override
    public double level() { return artronAmount; }

    @Override
    public void setLevel(double level) {
        this.artronAmount = Math.min(level, MAX_STORAGE);
        markDirty();
        sync();
    }

    @Override
    public double maxLevel() { return MAX_STORAGE; }

    @Override
    public void removeLevel(double amount) {
        this.artronAmount = Math.max(0, this.artronAmount - amount);
        markDirty();
        sync();
    }

    @Override
    public void addLevel(double amount) {
        this.artronAmount = Math.min(this.artronAmount + amount, MAX_STORAGE);
        markDirty();
        sync();
    }

    @Override
    public void onChange(double before, double after) { /* 可留空 */ }

    @Override
    public IFluidSource source(boolean search) {
        return this;
    }

    @Override
    public void setSource(IFluidSource source) { /* 作为源不需要设置 */ }

    @Override
    public IFluidLink last() {
        return null;
    }

    @Override
    public void setLast(IFluidLink last) { /* 作为源不需要设置 */ }

    // ====== ArtronHolder 实现 ======
    @Override
    public double getCurrentFuel() { return artronAmount; }

    @Override
    public void setCurrentFuel(double fuel) {
        this.artronAmount = Math.min(fuel, MAX_STORAGE);
        markDirty();
        sync();
    }

    @Override
    public double getMaxFuel() { return MAX_STORAGE; }

    @Override
    public void removeFuel(double amount) {
        this.artronAmount = Math.max(0, this.artronAmount - amount);
        markDirty();
        sync();
    }

    @Override
    public double addFuel(double amount) {
        double added = Math.min(amount, MAX_STORAGE - this.artronAmount);
        this.artronAmount += added;
        markDirty();
        sync();
        return amount - added;
    }

    // ====== NBT ======
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.yOffset = nbt.getFloat("YOffset");
        this.scale = nbt.getFloat("Scale");
        if (this.scale == 0.0f) this.scale = 1.0f;
        this.active = nbt.getBoolean("Active");
        this.eyeVisible = nbt.getBoolean("EyeVisible");
        this.redstoneMode = nbt.getBoolean("RedstoneMode");
        this.artronAmount = nbt.getDouble("ArtronAmount");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("YOffset", yOffset);
        nbt.putFloat("Scale", scale);
        nbt.putBoolean("Active", active);
        nbt.putBoolean("EyeVisible", eyeVisible);
        nbt.putBoolean("RedstoneMode", redstoneMode);
        nbt.putDouble("ArtronAmount", artronAmount);
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