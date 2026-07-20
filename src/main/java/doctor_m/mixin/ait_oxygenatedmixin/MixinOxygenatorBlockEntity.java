package doctor_m.mixin.ait_oxygenatedmixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.blockentities.OxygenatorBlockEntity;
import doctor_m.config.ConfigManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;

@Mixin(OxygenatorBlockEntity.class)
public class MixinOxygenatorBlockEntity {

    // 按世界隔离缓存，世界卸载自动释放
    private static final Map<World, Map<BlockPos, CachedRoom>> WORLD_CACHES = new WeakHashMap<>();

    @Overwrite
    public void tick(World world, BlockPos pos, BlockState state, OxygenatorBlockEntity self) {
        if (world.isClient()) return;
        if (world.getTime() % 40 != 0) return;

        var config = ConfigManager.getConfig();
        int cacheExpireTicks = config.oxygenatorCacheExpireTicks;

        // 获取或计算房间（包含合并逻辑）
        CachedRoom room = getOrComputeRoom(world, pos, cacheExpireTicks);
        if (room == null || room.airBlocks.isEmpty()) return;

        // 给房间内所有生物供氧
        List<LivingEntity> livingEntities = world.getEntitiesByClass(
                LivingEntity.class,
                room.boundingBox,
                e -> e.isAlive() && !e.isSpectator()
        );

        for (LivingEntity entity : livingEntities) {
            BlockPos entityPos = entity.getBlockPos();
            BlockPos headPos = entityPos.up();

            // 防穿墙核心：只有站在"可达空气"上的生物才能获得氧气
            if (!room.airBlocks.contains(entityPos) && !room.airBlocks.contains(headPos)) {
                continue;
            }

            entity.addStatusEffect(new StatusEffectInstance(
                    AITStatusEffects.OXYGENATED,
                    100, // 5秒，覆盖2次tick间隔
                    0,
                    false,
                    false
            ));
        }
    }

    // ========== 缓存管理 ==========

    private CachedRoom getOrComputeRoom(World world, BlockPos pos, int cacheExpireTicks) {
        long currentTime = world.getTime();
        var cache = WORLD_CACHES.computeIfAbsent(world, w -> new HashMap<>());

        CachedRoom cached = cache.get(pos);
        if (cached != null && currentTime - cached.lastUpdateTick < cacheExpireTicks) {
            return cached;
        }

        CachedRoom newRoom = computeRoom(world, pos);
        if (newRoom == null) {
            cache.remove(pos);
            return null;
        }

        // 尝试与附近氧气机合并（同一封闭空间共享范围）
        CachedRoom merged = tryMergeWithNearby(world, pos, newRoom, cache, cacheExpireTicks);
        if (merged != null) {
            for (BlockPos oxyPos : merged.oxygenatorPositions) {
                cache.put(oxyPos, merged);
            }
            return merged;
        }

        cache.put(pos, newRoom);
        return newRoom;
    }

    // ========== 核心 Flood Fill（距离限制版） ==========

    private CachedRoom computeRoom(World world, BlockPos start) {
        var config = ConfigManager.getConfig();
        int maxRadius = config.oxygenatorMaxRadius;        // 新增配置：最大供氧半径
        int openSpaceRadius = config.oxygenatorOpenSpaceRadius;
        int minAirBlocks = config.oxygenatorMinAirBlocks;

        Set<BlockPos> airBlocks = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        // 从氧气机周围6个邻居开始扩散（氧气机本身不是空气）
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = start.offset(dir);
            if (isAir(world, neighbor)) {
                queue.add(neighbor);
                airBlocks.add(neighbor);
            }
        }

        // 如果连邻居都不是空气（被埋了），应急处理
        if (airBlocks.isEmpty()) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos p = start.add(x, y, z);
                        if (isAir(world, p)) airBlocks.add(p);
                    }
                }
            }
            if (airBlocks.isEmpty()) {
                return new CachedRoom(Collections.emptySet(), null, world.getTime(), false, start);
            }
            return new CachedRoom(airBlocks, computeBoundingBox(airBlocks), world.getTime(), false, start);
        }

        boolean truncated = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // ====== 关键：按距离截断，不是按数量 ======
            if (Math.abs(current.getX() - start.getX()) >= maxRadius ||
                    Math.abs(current.getY() - start.getY()) >= maxRadius ||
                    Math.abs(current.getZ() - start.getZ()) >= maxRadius) {
                truncated = true;
                continue; // 这个方向不再扩散，但继续处理其他方向
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.offset(dir);
                if (airBlocks.contains(neighbor)) continue;

                if (isAir(world, neighbor)) {
                    airBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // ====== 开放空间判定 ======
        if (truncated) {
            // 被距离截断：保守处理，只保留氧气机周围小范围
            Set<BlockPos> smallRange = new HashSet<>();
            for (int x = -openSpaceRadius; x <= openSpaceRadius; x++) {
                for (int y = -openSpaceRadius; y <= openSpaceRadius; y++) {
                    for (int z = -openSpaceRadius; z <= openSpaceRadius; z++) {
                        BlockPos p = start.add(x, y, z);
                        if (isAir(world, p)) smallRange.add(p);
                    }
                }
            }
            return new CachedRoom(smallRange, computeBoundingBox(smallRange), world.getTime(), false, start);
        }

        // 队列自然排空 = 封闭空间
        if (airBlocks.size() < minAirBlocks) {
            return new CachedRoom(Collections.emptySet(), null, world.getTime(), true, start);
        }

        return new CachedRoom(airBlocks, computeBoundingBox(airBlocks), world.getTime(), true, start);
    }

    // ========== 多氧气机合并 ==========

    private CachedRoom tryMergeWithNearby(World world, BlockPos pos, CachedRoom room,
                                          Map<BlockPos, CachedRoom> cache, int cacheExpireTicks) {
        if (!room.isEnclosed) return null;

        CachedRoom bestMerge = null;

        for (Map.Entry<BlockPos, CachedRoom> entry : cache.entrySet()) {
            BlockPos otherPos = entry.getKey();
            CachedRoom other = entry.getValue();

            if (otherPos.equals(pos)) continue;
            if (!other.isEnclosed) continue;
            if (world.getTime() - other.lastUpdateTick >= cacheExpireTicks) continue;
            if (!room.boundingBox.intersects(other.boundingBox)) continue;

            // 确认真的连通：检查两个房间是否有共享空气块
            Set<BlockPos> intersection = new HashSet<>(room.airBlocks);
            intersection.retainAll(other.airBlocks);
            if (intersection.isEmpty()) continue;

            bestMerge = other;
            break;
        }

        if (bestMerge == null) return null;

        // 合并
        Set<BlockPos> mergedAir = new HashSet<>(bestMerge.airBlocks);
        mergedAir.addAll(room.airBlocks);

        Set<BlockPos> mergedOxy = new HashSet<>(bestMerge.oxygenatorPositions);
        mergedOxy.add(pos);

        return new CachedRoom(mergedAir, computeBoundingBox(mergedAir), world.getTime(), true, mergedOxy);
    }

    // ========== 工具方法 ==========

    private static boolean isAir(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.getBlock() == Blocks.CAVE_AIR;
    }

    private static Box computeBoundingBox(Set<BlockPos> blocks) {
        if (blocks.isEmpty()) return null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : blocks) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getZ() < minZ) minZ = p.getZ();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
            if (p.getZ() > maxZ) maxZ = p.getZ();
        }
        return new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    // ========== 缓存数据结构 ==========

    private static class CachedRoom {
        final Set<BlockPos> airBlocks;
        final Box boundingBox;
        final long lastUpdateTick;
        final boolean isEnclosed;
        final Set<BlockPos> oxygenatorPositions;

        CachedRoom(Set<BlockPos> airBlocks, Box boundingBox, long lastUpdateTick,
                   boolean isEnclosed, BlockPos... oxygenators) {
            this.airBlocks = airBlocks;
            this.boundingBox = boundingBox;
            this.lastUpdateTick = lastUpdateTick;
            this.isEnclosed = isEnclosed;
            this.oxygenatorPositions = new HashSet<>(Arrays.asList(oxygenators));
        }

        CachedRoom(Set<BlockPos> airBlocks, Box boundingBox, long lastUpdateTick,
                   boolean isEnclosed, Set<BlockPos> oxygenators) {
            this.airBlocks = airBlocks;
            this.boundingBox = boundingBox;
            this.lastUpdateTick = lastUpdateTick;
            this.isEnclosed = isEnclosed;
            this.oxygenatorPositions = oxygenators;
        }
    }
}