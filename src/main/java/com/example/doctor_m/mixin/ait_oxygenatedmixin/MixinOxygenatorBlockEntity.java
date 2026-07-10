package com.example.doctor_m.mixin.ait_oxygenatedmixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.blockentities.OxygenatorBlockEntity;
import doctor_m.util.config.ConfigManager;
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
    // 移除硬编码常量

    @Overwrite
    public void tick(World world, BlockPos pos, BlockState state, OxygenatorBlockEntity self) {
        if (world.isClient()) return;
        if (world.getTime() % 40 != 0) return;

        var config = ConfigManager.getConfig();
        int detectionRadius = config.oxygenatorBiologicalDetectionRadius;

        // 检测周围生物
        boolean hasLivingEntityNearby = !world.getEntitiesByClass(LivingEntity.class,
                new Box(pos).expand(detectionRadius), e -> !e.isSpectator() && e.isAlive()).isEmpty();
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
        var config = ConfigManager.getConfig();
        int cacheExpireTicks = config.oxygenatorCacheExpireTicks;

        CachedRoom cached = roomCache.get(pos);
        if (cached != null && currentTime - cached.lastUpdateTick < cacheExpireTicks) {
            return cached;
        }

        CachedRoom newRoom = computeRoom(world, pos);
        roomCache.put(pos, newRoom);
        return newRoom;
    }

    private CachedRoom computeRoom(World world, BlockPos start) {
        var config = ConfigManager.getConfig();
        int maxSearchSize = config.oxygenatorMaxSearchSize;
        int minAirBlocks = config.oxygenatorMinAirBlocks;

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

        while (!queue.isEmpty() && airBlocks.size() < maxSearchSize) {
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

        if (airBlocks.size() < minAirBlocks) {
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