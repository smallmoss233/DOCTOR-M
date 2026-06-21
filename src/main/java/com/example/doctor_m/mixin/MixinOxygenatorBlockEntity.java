package com.example.doctor_m.mixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.blockentities.OxygenatorBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;

@Mixin(OxygenatorBlockEntity.class)
public class MixinOxygenatorBlockEntity {

    private static final Map<BlockPos, CachedRoom> roomCache = new HashMap<>();
    private static final int CACHE_EXPIRE_TICKS = 40;
    private static final int MAX_SEARCH_SIZE = 5000;
    private static final int MIN_AIR_BLOCKS = 10;

    @Overwrite
    public void tick(World world, BlockPos pos, BlockState state, OxygenatorBlockEntity self) {
        if (world.isClient()) return;
        if (world.getTime() % 40 != 0) return;

        // 检测周围 16 格内是否有生物（玩家、动物、怪物等）
        boolean hasLivingEntityNearby = !world.getEntitiesByClass(LivingEntity.class,
                new Box(pos).expand(16), e -> !e.isSpectator() && e.isAlive()).isEmpty();
        if (!hasLivingEntityNearby) return;

        // 获取或计算封闭空间
        CachedRoom room = getOrComputeRoom(world, pos);
        if (room == null || room.airBlocks.isEmpty()) return;

        // 获取封闭空间内的所有生物
        Box roomBox = room.boundingBox;
        List<LivingEntity> livingEntities = world.getEntitiesByClass(LivingEntity.class, roomBox,
                e -> e.isAlive() && !e.isSpectator());

        // 为所有生物施加有氧状态
        for (LivingEntity entity : livingEntities) {
            if (!room.airBlocks.contains(entity.getBlockPos())) continue;
            entity.addStatusEffect(new StatusEffectInstance(
                    AITStatusEffects.OXYGENATED,
                    60,
                    0,
                    false,
                    false
            ));
        }
    }

    // 洪水填充计算边界
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

        int[][] directions = {
                {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
        };

        int minX = start.getX(), maxX = start.getX();
        int minY = start.getY(), maxY = start.getY();
        int minZ = start.getZ(), maxZ = start.getZ();

        while (!queue.isEmpty() && airBlocks.size() < MAX_SEARCH_SIZE) {
            BlockPos current = queue.poll();
            int x = current.getX(), y = current.getY(), z = current.getZ();

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
                if (neighborState.isAir() || neighborState.getBlock() == Blocks.CAVE_AIR) {
                    airBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

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