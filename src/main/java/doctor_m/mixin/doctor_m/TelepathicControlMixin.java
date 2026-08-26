package doctor_m.mixin.doctor_m;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.item.KeyItem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.impl.TelepathicControl;
import dev.amble.ait.core.tardis.util.AsyncLocatorUtil;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import doctor_m.Item.KeytoTime;
import doctor_m.Item.data_item.TracerItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(TelepathicControl.class)
public abstract class TelepathicControlMixin {

    private static final double SCAN_RANGE = 5120.0;
    private static final double SCAN_RANGE_SQ = SCAN_RANGE * SCAN_RANGE;
    private static final int STRUCTURE_SEARCH_RADIUS = 5120;

    // 已探索结构黑名单：Tardis UUID -> 结构坐标集合
    private static final Map<UUID, Set<BlockPos>> EXPLORED_STRUCTURES = new HashMap<>();
    // 结构坐标容差（128格内视为同一结构）
    private static final int BLACKLIST_TOLERANCE = 128;
    private static final int BLACKLIST_TOLERANCE_SQ = BLACKLIST_TOLERANCE * BLACKLIST_TOLERANCE;
    // 链式搜索最大尝试次数
    private static final int MAX_CHAIN_ATTEMPTS = 5;

    private static final TagKey<Structure> KTT_STRUCTURES = TagKey.of(
            RegistryKeys.STRUCTURE,
            new Identifier("doctor_m", "ktt_fragment_structures")
    );

    @Inject(method = "runServer", at = @At("HEAD"), cancellable = true)
    private void doctor_m$tracerMode(Tardis tardis, ServerPlayerEntity player, ServerWorld world,
                                     BlockPos console, boolean leftClick,
                                     CallbackInfoReturnable<Control.Result> cir) {

        if (tardis.stats().security().get() && !KeyItem.hasMatchingKeyInInventory(player, tardis)) {
            return;
        }

        if (!(world.getBlockEntity(console) instanceof ConsoleBlockEntity consoleBe)) {
            return;
        }

        if (!(consoleBe.getSonicScrewdriver().getItem() instanceof TracerItem)) {
            return;
        }

        // ===== 潜行右键：标记当前最近的远古结构为已探索 =====
        if (player.isSneaking()) {
            cir.setReturnValue(doctor_m$markAndExclude(tardis, player, world, console));
            return;
        }

        // ===== 普通右键：搜索并锁定 =====
        cir.setReturnValue(doctor_m$searchAndLock(tardis, player, world, console));
    }

    // ========== 标记当前结构为已探索 ==========

    private static Control.Result doctor_m$markAndExclude(Tardis tardis, ServerPlayerEntity player,
                                                          ServerWorld consoleWorld, BlockPos console) {
        CachedDirectedGlobalPos exterior = tardis.travel().position();
        if (exterior == null) {
            player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.tardis_position_unknown"), true);
            return Control.Result.FAILURE;
        }

        ServerWorld extWorld = exterior.getWorld();
        if (extWorld == null) {
            player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.exterior_unavailable"), true);
            return Control.Result.FAILURE;
        }

        player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.marking"), true);
        consoleWorld.playSound(null, console,
                SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.8f, 1.2f);

        AsyncLocatorUtil.locate(extWorld, KTT_STRUCTURES, exterior.getPos(),
                STRUCTURE_SEARCH_RADIUS, false).thenOnServerThread(result -> {

            if (result == null) {
                player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.no_structure_to_mark"), true);
                return;
            }

            addToBlacklist(tardis, result);
            consoleWorld.playSound(null, console,
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.BLOCKS, 1f, 2f);
            player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.structure_marked",
                    result.getX(), result.getY(), result.getZ()), true);
        });

        return Control.Result.SUCCESS;
    }

    // ========== 黑名单工具方法 ==========

    private static UUID getTardisId(Tardis tardis) {
        return tardis.getUuid();
    }

    private static void addToBlacklist(Tardis tardis, BlockPos pos) {
        EXPLORED_STRUCTURES.computeIfAbsent(getTardisId(tardis), k -> new HashSet<>()).add(pos);
    }

    private static boolean isBlacklisted(Tardis tardis, BlockPos pos) {
        Set<BlockPos> list = EXPLORED_STRUCTURES.get(getTardisId(tardis));
        if (list == null || list.isEmpty()) return false;

        for (BlockPos blacklisted : list) {
            double dx = pos.getX() - blacklisted.getX();
            double dy = pos.getY() - blacklisted.getY();
            double dz = pos.getZ() - blacklisted.getZ();
            if (dx * dx + dy * dy + dz * dz < BLACKLIST_TOLERANCE_SQ) {
                return true;
            }
        }
        return false;
    }

    // ========== 搜索并锁定（原有逻辑 + 链式结构搜索） ==========

    private static Control.Result doctor_m$searchAndLock(Tardis tardis, ServerPlayerEntity player,
                                                         ServerWorld consoleWorld, BlockPos console) {
        CachedDirectedGlobalPos exterior = tardis.travel().position();
        if (exterior == null) {
            player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.tardis_position_unknown"), true);
            return Control.Result.FAILURE;
        }

        ServerWorld extWorld = exterior.getWorld();
        if (extWorld == null) {
            player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.exterior_unavailable"), true);
            return Control.Result.FAILURE;
        }

        Vec3d center = Vec3d.ofCenter(exterior.getPos());
        double bestSq = SCAN_RANGE_SQ + 1;
        BlockPos foundPos = null;
        boolean foundInContainer = false;
        Box box = new Box(center, center).expand(SCAN_RANGE);

        // 1. 掉落物
        for (ItemEntity item : extWorld.getEntitiesByClass(
                ItemEntity.class, box,
                e -> e.getStack().getItem() instanceof KeytoTime)) {

            double d = item.squaredDistanceTo(center);
            if (d < bestSq) {
                bestSq = d;
                foundPos = item.getBlockPos();
                foundInContainer = false;
            }
        }

        // 1.5 物品展示框
        for (ItemFrameEntity frame : extWorld.getEntitiesByClass(
                ItemFrameEntity.class, box,
                e -> e.getHeldItemStack().getItem() instanceof KeytoTime)) {

            double d = frame.getPos().squaredDistanceTo(center);
            if (d < bestSq) {
                bestSq = d;
                foundPos = frame.getBlockPos();
                foundInContainer = false;
            }
        }

        // 1.6 生物携带
        for (LivingEntity living : extWorld.getEntitiesByClass(
                LivingEntity.class, box,
                e -> {
                    if (e instanceof Inventory inv) {
                        for (int i = 0; i < inv.size(); i++) {
                            if (inv.getStack(i).getItem() instanceof KeytoTime) return true;
                        }
                    }
                    return e.getMainHandStack().getItem() instanceof KeytoTime
                            || e.getOffHandStack().getItem() instanceof KeytoTime;
                })) {

            double d = living.getPos().squaredDistanceTo(center);
            if (d < bestSq) {
                bestSq = d;
                foundPos = living.getBlockPos();
                foundInContainer = false;
            }
        }

        // 2. 容器
        if (foundPos == null || bestSq > 256) {
            int cX = exterior.getPos().getX() >> 4;
            int cZ = exterior.getPos().getZ() >> 4;
            int range = (int) (SCAN_RANGE / 16) + 1;

            for (int cx = cX - range; cx <= cX + range; cx++) {
                for (int cz = cZ - range; cz <= cZ + range; cz++) {
                    WorldChunk chunk = extWorld.getChunkManager().getWorldChunk(cx, cz, false);
                    if (chunk == null) continue;

                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof Inventory inv)) continue;

                        double d = Vec3d.ofCenter(be.getPos()).squaredDistanceTo(center);
                        if (d > SCAN_RANGE_SQ) continue;

                        for (int i = 0; i < inv.size(); i++) {
                            if (inv.getStack(i).getItem() instanceof KeytoTime) {
                                if (d < bestSq) {
                                    bestSq = d;
                                    foundPos = be.getPos();
                                    foundInContainer = true;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 3. 实时目标找到 → 直接设航线
        if (foundPos != null) {
            setDestination(tardis, player, consoleWorld, console, extWorld, foundPos, foundInContainer);
            return Control.Result.SUCCESS;
        }

        // 4. 没找到 → 异步链式搜结构（自动跳过已标记）
        player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.scanning_structures"), true);
        consoleWorld.playSound(null, console,
                SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 1f, 0.7f);

        searchStructuresAsync(tardis, player, consoleWorld, console, extWorld, exterior.getPos());
        return Control.Result.SUCCESS;
    }

    private static void setDestination(Tardis tardis, ServerPlayerEntity player,
                                       ServerWorld consoleWorld, BlockPos console,
                                       ServerWorld extWorld, BlockPos targetPos,
                                       boolean inContainer) {
        BlockPos dest = targetPos.add(
                extWorld.random.nextInt(80) - 40,
                0,
                extWorld.random.nextInt(80) - 40
        );

        tardis.travel().forceDestination(
                CachedDirectedGlobalPos.create(
                        extWorld.getRegistryKey(),
                        dest,
                        (byte) extWorld.random.nextInt(16)
                )
        );
        tardis.removeFuel(300);

        consoleWorld.playSound(null, console,
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1f, 2f);

        String typeKey = inContainer
                ? "tooltip.doctor_m.tracer.signal_locked.container"
                : "tooltip.doctor_m.tracer.signal_locked.surface";
        player.sendMessage(Text.translatable(typeKey), true);
    }

    // ========== 链式结构搜索（自动跳过已标记） ==========

    private static void searchStructuresAsync(Tardis tardis, ServerPlayerEntity player,
                                              ServerWorld consoleWorld, BlockPos console,
                                              ServerWorld extWorld, BlockPos searchCenter) {
        var holderSet = extWorld.getRegistryManager()
                .get(RegistryKeys.STRUCTURE)
                .getEntryList(KTT_STRUCTURES);

        if (holderSet.isEmpty() || holderSet.get().size() == 0) {
            player.sendMessage(Text.translatable(
                    "tooltip.doctor_m.tracer.no_structure_tag",
                    "doctor_m:ktt_fragment_structures"
            ), true);
            return;
        }

        tryLocateNext(tardis, player, consoleWorld, console, extWorld,
                searchCenter, searchCenter, STRUCTURE_SEARCH_RADIUS, null, MAX_CHAIN_ATTEMPTS);
    }

    private static void tryLocateNext(Tardis tardis, ServerPlayerEntity player,
                                      ServerWorld consoleWorld, BlockPos console,
                                      ServerWorld extWorld, BlockPos originalCenter,
                                      BlockPos currentCenter, int radius,
                                      @Nullable BlockPos excludePos, int attemptsLeft) {

        if (attemptsLeft <= 0) {
            consoleWorld.playSound(null, console,
                    SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.BLOCKS, 1f, 0.5f);
            player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.all_structures_explored"), true);
            return;
        }

        AsyncLocatorUtil.locate(extWorld, KTT_STRUCTURES, currentCenter, radius, false)
                .thenOnServerThread(result -> {
                    if (result == null) {
                        consoleWorld.playSound(null, console,
                                SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.BLOCKS, 1f, 0.5f);
                        player.sendMessage(Text.translatable(
                                "tooltip.doctor_m.tracer.no_structure_signal",
                                STRUCTURE_SEARCH_RADIUS
                        ), true);
                        return;
                    }

                    // 检查 locate 是否又返回了同一个结构（递归时）
                    if (excludePos != null
                            && result.getSquaredDistance(excludePos) < BLACKLIST_TOLERANCE_SQ) {
                        // 偏移搜索中心后重试
                        BlockPos offset = currentCenter.add(
                                extWorld.random.nextInt(600) - 300,
                                0,
                                extWorld.random.nextInt(600) - 300
                        );
                        tryLocateNext(tardis, player, consoleWorld, console, extWorld,
                                originalCenter, offset, radius, excludePos, attemptsLeft - 1);
                        return;
                    }

                    // 检查是否在原始搜索半径内
                    double distToOriginal = result.getSquaredDistance(originalCenter);
                    if (distToOriginal > (long) STRUCTURE_SEARCH_RADIUS * STRUCTURE_SEARCH_RADIUS) {
                        consoleWorld.playSound(null, console,
                                SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.BLOCKS, 1f, 0.5f);
                        player.sendMessage(Text.translatable(
                                "tooltip.doctor_m.tracer.no_structure_signal",
                                STRUCTURE_SEARCH_RADIUS
                        ), true);
                        return;
                    }

                    // 检查黑名单
                    if (isBlacklisted(tardis, result)) {
                        player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.skipping_explored"), true);
                        // 以该结构为中心继续搜索下一个
                        tryLocateNext(tardis, player, consoleWorld, console, extWorld,
                                originalCenter, result, radius, result, attemptsLeft - 1);
                        return;
                    }

                    // 找到了未标记的结构，锁定
                    BlockPos dest = result.add(
                            extWorld.random.nextInt(80) - 40,
                            0,
                            extWorld.random.nextInt(80) - 40
                    );

                    tardis.travel().forceDestination(
                            CachedDirectedGlobalPos.create(
                                    extWorld.getRegistryKey(),
                                    dest,
                                    (byte) extWorld.random.nextInt(16)
                            )
                    );
                    tardis.removeFuel(600);

                    consoleWorld.playSound(null, console,
                            SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1f, 1.8f);
                    player.sendMessage(Text.translatable("tooltip.doctor_m.tracer.structure_locked"), true);
                });
    }
}