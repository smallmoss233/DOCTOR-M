package doctor_m.block.entities;

import dev.amble.ait.core.AITItems;
import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.drinks.DrinkRegistry;
import dev.amble.ait.core.drinks.DrinkUtil;
import doctor_m.block.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoffeeMachineBlockEntity extends BlockEntity {
    private static final int WORK_DURATION = 45;
    private int currentDrinkIndex = 0;
    private boolean working = false;
    private int workTicks = 0;
    private int pendingDrinkIndex = -1;

    public CoffeeMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COFFEE_MACHINE, pos, state);
    }

    public int getCurrentDrinkIndex() {
        return currentDrinkIndex;
    }

    public boolean isWorking() {
        return working;
    }

    public void nextDrink() {
        List<?> drinks = DrinkRegistry.getInstance().toList();
        currentDrinkIndex = drinks.isEmpty() ? 0 : (currentDrinkIndex + 1) % drinks.size();
        markDirtyAndSync();
    }

    public void startWorking(int drinkIndex) {
        this.working = true;
        this.workTicks = 0;
        this.pendingDrinkIndex = drinkIndex;

        // 放入杯子，播放咖啡机启动声
        if (world != null && !world.isClient) {
            world.playSound(null, pos, AITSounds.COFFEE_MACHINE,
                    SoundCategory.BLOCKS, 1.0f, 1.0f);
        }

        markDirtyAndSync();
    }

    public static void tick(World world, BlockPos pos, BlockState state, CoffeeMachineBlockEntity be) {
        if (world.isClient || !be.working) return;

        be.workTicks++;

        if (be.workTicks % 5 == 0) {
            spawnParticles((ServerWorld) world, pos);
        }

        if (be.workTicks >= WORK_DURATION) {
            be.finishWork((ServerWorld) world, pos);
        }
    }

    private static void spawnParticles(ServerWorld world, BlockPos pos) {
        double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.4;
        double y = pos.getY() + 0.9 + world.random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.4;
        world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0.05, 0, 0.02);
    }

    private void finishWork(ServerWorld world, BlockPos pos) {
        this.working = false;
        this.workTicks = 0;

        if (pendingDrinkIndex >= 0) {
            // 完成后弹出饮品，播放原版弹出音效
            world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_DISPENSE,
                    SoundCategory.BLOCKS, 1.0f, 1.0f);

            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    12, 0.4, 0.3, 0.4, 0.05);

            var drinks = DrinkRegistry.getInstance().toList();
            if (!drinks.isEmpty() && pendingDrinkIndex < drinks.size()) {
                ItemStack mug = DrinkUtil.setDrink(new ItemStack(AITItems.MUG), drinks.get(pendingDrinkIndex));

                Vec3d spawnPos = Vec3d.ofCenter(pos).add(0, 0.7, 0);
                ItemEntity entity = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, mug);
                entity.setToDefaultPickupDelay();
                entity.setVelocity(
                        world.random.nextDouble() * 0.04 - 0.02,
                        0.15,
                        world.random.nextDouble() * 0.04 - 0.02
                );
                world.spawnEntity(entity);
            }
        }

        this.pendingDrinkIndex = -1;
        markDirtyAndSync();
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("DrinkIndex", currentDrinkIndex);
        nbt.putBoolean("Working", working);
        nbt.putInt("WorkTicks", workTicks);
        if (pendingDrinkIndex >= 0) {
            nbt.putInt("PendingIndex", pendingDrinkIndex);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        currentDrinkIndex = nbt.getInt("DrinkIndex");
        working = nbt.getBoolean("Working");
        workTicks = nbt.getInt("WorkTicks");
        pendingDrinkIndex = nbt.contains("PendingIndex") ? nbt.getInt("PendingIndex") : -1;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}