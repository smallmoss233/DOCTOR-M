package com.example.doctor_m.mixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.blockentities.OxygenatorBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;

@Mixin(OxygenatorBlockEntity.class)
public class MixinOxygenatorBlockEntity {

    // 缓存上次的计算结果
    private static final Map<BlockPos, CachedRoom> roomCache = new HashMap<>();
    private static final int CACHE_EXPIRE_TICKS = 40; // 每 40 tick 刷新一次（2 秒）
    private static final int MAX_SEARCH_SIZE = 5000; // 最大搜索空气块数量，防止死循环
    private static final int MIN_AIR_BLOCKS = 10; // 最小空气块数量，避免把一个小洞当成房间

    @Overwrite
    public void tick(World world, BlockPos pos, BlockState state, OxygenatorBlockEntity self) {
        if (world.isClient()) return;

        // 1. 每 40 tick 检测一次
        if (world.getTime() % 40 != 0) return;

        // 2. 先检测周围 16 格内是否有玩家
        boolean hasPlayerNearby = !world.getEntitiesByClass(PlayerEntity.class,
                new Box(pos).expand(16), e -> !e.isSpectator() && e.isAlive()).isEmpty();
        if (!hasPlayerNearby) return;

        // 3. 获取或计算封闭空间
        CachedRoom room = getOrComputeRoom(world, pos);
        if (room == null || room.airBlocks.isEmpty()) return;

        // 4. 获取封闭空间内的玩家
        Box roomBox = room.boundingBox;
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, roomBox,
                e -> !e.isSpectator() && e.isAlive());

        // 5. 为所有玩家施加有氧状态（无论是否穿宇航服）
        for (PlayerEntity player : players) {
            // 检查玩家是否真的在封闭空间内（进一步确认）
            if (!room.airBlocks.contains(player.getBlockPos())) continue;

            // 施加 OXYGENATED 效果，持续 1 秒（20 tick）
            player.addStatusEffect(new StatusEffectInstance(
                    AITStatusEffects.OXYGENATED,
                    20,
                    0,
                    false,
                    false
            ));
        }
    }

    // 以下方法保持不变（Room 检测逻辑）
    private CachedRoom getOrComputeRoom(World world, BlockPos pos) {
        long currentTime = world.getTime();
        CachedRoom cached = roomCache.get(pos);
        if (cached != null && currentTime - cached.lastUpdateTick < CACHE_EXPIRE_TICKS) {
            return cached;
        }

        CachedRoom newRoom = computeRoom(world, pos);
        roomCache.put(pos, newRoom);
        return newRoom;
    }

    private CachedRoom computeRoom(World world, BlockPos start) {
        Set<BlockPos> airBlocks = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        airBlocks.add(start);

        // 六个方向：上、下、左、右、前、后
        int[][] directions = {
                {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
        };

        int minX = start.getX(), maxX = start.getX();
        int minY = start.getY(), maxY = start.getY();
        int minZ = start.getZ(), maxZ = start.getZ();

        while (!queue.isEmpty() && airBlocks.size() < MAX_SEARCH_SIZE) {
            BlockPos current = queue.poll();
            int x = current.getX(), y = current.getY(), z = current.getZ();

            // 更新边界
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;

            for (int[] dir : directions) {
                BlockPos neighbor = new BlockPos(x + dir[0], y + dir[1], z + dir[2]);
                if (airBlocks.contains(neighbor)) continue;

                BlockState neighborState = world.getBlockState(neighbor);
                // 如果是空气（或可替换方块），加入队列
                if (neighborState.isAir() || neighborState.getBlock() == Blocks.CAVE_AIR) {
                    airBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // 如果空气块太少，认为不是有效房间
        if (airBlocks.size() < MIN_AIR_BLOCKS) {
            return new CachedRoom(Collections.emptySet(), null, world.getTime());
        }

        Box boundingBox = new Box(
                new BlockPos(minX - 1, minY - 1, minZ - 1),
                new BlockPos(maxX + 1, maxY + 1, maxZ + 1)
        );
        return new CachedRoom(airBlocks, boundingBox, world.getTime());
    }

    private static class CachedRoom {
        final Set<BlockPos> airBlocks;
        final Box boundingBox;
        final long lastUpdateTick;

        CachedRoom(Set<BlockPos> airBlocks, Box boundingBox, long lastUpdateTick) {
            this.airBlocks = airBlocks;
            this.boundingBox = boundingBox;
            this.lastUpdateTick = lastUpdateTick;
        }
    }
}