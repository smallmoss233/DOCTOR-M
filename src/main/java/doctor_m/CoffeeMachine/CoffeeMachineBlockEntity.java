package doctor_m.CoffeeMachine;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
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
import org.jetbrains.annotations.Nullable;

public class CoffeeMachineBlockEntity extends BlockEntity {
    private int currentDrinkIndex = 0;
    private boolean isWorking = false;
    private int workTicks = 0;
    private static final int WORK_DURATION = 40; // 2秒（40tick = 2秒）
    private String pendingDrinkId = "";

    public CoffeeMachineBlockEntity(BlockPos pos, BlockState state) {
        super(doctor_m.block.entity.DOCTORMBlockEntities.COFFEE_MACHINE_BLOCK_ENTITY, pos, state);
    }

    public int getCurrentDrinkIndex() {
        return currentDrinkIndex;
    }

    public void setCurrentDrinkIndex(int index) {
        this.currentDrinkIndex = index;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public void nextDrink() {
        setCurrentDrinkIndex((currentDrinkIndex + 1) % 9); // 总共9种饮品
    }

    public boolean isWorking() {
        return isWorking;
    }

    public void startWorking(String drinkId) {
        this.isWorking = true;
        this.workTicks = 0;
        this.pendingDrinkId = drinkId;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public void tick() {
        if (isWorking && world != null && !world.isClient) {
            workTicks++;

            // 播放工作粒子效果
            if (workTicks % 5 == 0) {
                ServerWorld serverWorld = (ServerWorld) world;
                BlockPos pos = getPos();

                // 在咖啡机上方产生粒子
                for (int i = 0; i < 3; i++) {
                    double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;
                    double y = pos.getY() + 1.0 + world.random.nextDouble() * 0.5;
                    double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;

                    serverWorld.spawnParticles(ParticleTypes.SMOKE,
                            x, y, z,
                            1, 0, 0, 0, 0.02);
                }
            }

            // 工作完成
            if (workTicks >= WORK_DURATION) {
                finishWork();
            }
        }
    }

    private void finishWork() {
        isWorking = false;
        workTicks = 0;

        if (world != null && !world.isClient && !pendingDrinkId.isEmpty()) {
            ServerWorld serverWorld = (ServerWorld) world;
            BlockPos pos = getPos();

            // 播放完成音效
            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.BLOCKS, 0.5f, 1.0f);

            // 播放绿色粒子效果
            for (int i = 0; i < 15; i++) {
                double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 1.5;
                double y = pos.getY() + 0.8 + world.random.nextDouble() * 1.0;
                double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 1.5;

                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        x, y, z,
                        1, 0, 0, 0, 0.1);
            }

            // 给予玩家饮品（通过指令）
            giveDrinkToPlayer(pendingDrinkId);

            // 重要：不移除下一行！不切换到下一个饮品！只清除pendingDrinkId
            pendingDrinkId = "";
        }

        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    // 使用指令给予玩家饮品
    private void giveDrinkToPlayer(String drinkId) {
        if (world == null || world.isClient) return;

        // 找到最近的玩家
        var players = world.getPlayers();
        if (players.isEmpty()) return;

        // 找到距离咖啡机最近的玩家
        var nearestPlayer = players.stream()
                .min((p1, p2) -> {
                    double d1 = p1.getPos().distanceTo(Vec3d.of(pos));
                    double d2 = p2.getPos().distanceTo(Vec3d.of(pos));
                    return Double.compare(d1, d2);
                })
                .orElse(null);

        if (nearestPlayer == null) return;

        try {
            // 构建give指令
            String command = String.format("give %s ait:mug{Drink:\"%s\"} 1",
                    nearestPlayer.getName().getString(), drinkId);

            // 执行指令
            world.getServer().getCommandManager().executeWithPrefix(
                    world.getServer().getCommandSource()
                            .withSilent()
                            .withLevel(2)
                            .withEntity(nearestPlayer),
                    command
            );
        } catch (Exception e) {
            System.err.println("执行give指令失败: " + e.getMessage());
        }
    }

    public String getPendingDrinkId() {
        return pendingDrinkId;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("CurrentDrinkIndex", currentDrinkIndex);
        nbt.putBoolean("IsWorking", isWorking);
        nbt.putInt("WorkTicks", workTicks);
        if (!pendingDrinkId.isEmpty()) {
            nbt.putString("PendingDrinkId", pendingDrinkId);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        currentDrinkIndex = nbt.getInt("CurrentDrinkIndex");
        isWorking = nbt.getBoolean("IsWorking");
        workTicks = nbt.getInt("WorkTicks");
        if (nbt.contains("PendingDrinkId")) {
            pendingDrinkId = nbt.getString("PendingDrinkId");
        }
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