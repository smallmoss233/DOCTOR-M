package doctor_m.mixin.doctor_m;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.item.KeyItem;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.impl.TelepathicControl;
import dev.amble.ait.core.tardis.util.AsyncLocatorUtil;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import doctor_m.Item.KeytoTime;
import doctor_m.Item.data_itme.TracerItem;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TelepathicControl.class)
public abstract class TelepathicControlMixin {

    private static final double SCAN_RANGE = 5120.0;
    private static final double SCAN_RANGE_SQ = SCAN_RANGE * SCAN_RANGE;
    private static final int STRUCTURE_SEARCH_RADIUS = 5120;

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

        cir.setReturnValue(doctor_m$searchAndLock(tardis, player, world, console));
    }

    private static Control.Result doctor_m$searchAndLock(Tardis tardis, ServerPlayerEntity player,
                                                         ServerWorld consoleWorld, BlockPos console) {
        CachedDirectedGlobalPos exterior = tardis.travel().position();
        if (exterior == null) {
            player.sendMessage(Text.translatable("message.doctor_m.tracer.tardis_position_unknown"), true);
            return Control.Result.FAILURE;
        }

        ServerWorld extWorld = exterior.getWorld();
        if (extWorld == null) {
            player.sendMessage(Text.translatable("message.doctor_m.tracer.exterior_unavailable"), true);
            return Control.Result.FAILURE;
        }

        Vec3d center = Vec3d.ofCenter(exterior.getPos());
        double bestSq = SCAN_RANGE_SQ + 1;
        BlockPos foundPos = null;
        boolean foundInContainer = false;
        Box box = new Box(center, center).expand(SCAN_RANGE);

        // ===== 1. 同步搜已加载区域的掉落物 =====
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

        // ===== 1.5 同步搜已加载区域的物品展示框 =====
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

        // ===== 1.6 同步搜已加载区域的生物携带 =====
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

        // ===== 2. 同步搜已加载区域的容器 =====
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

        // ===== 3. 实时目标找到 → 直接设航线 =====
        if (foundPos != null) {
            setDestination(tardis, player, consoleWorld, console, extWorld, foundPos, foundInContainer);
            return Control.Result.SUCCESS;
        }

        // ===== 4. 没找到 → 异步搜标签内的结构 =====
        player.sendMessage(Text.translatable("message.doctor_m.tracer.scanning_structures"), true);
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
                extWorld.random.nextInt(160) - 80,
                0,
                extWorld.random.nextInt(160) - 80
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
                ? "message.doctor_m.tracer.signal_locked.container"
                : "message.doctor_m.tracer.signal_locked.surface";
        player.sendMessage(Text.translatable(typeKey), true);
    }

    private static void searchStructuresAsync(Tardis tardis, ServerPlayerEntity player,
                                              ServerWorld consoleWorld, BlockPos console,
                                              ServerWorld extWorld, BlockPos searchCenter) {
        var holderSet = extWorld.getRegistryManager()
                .get(RegistryKeys.STRUCTURE)
                .getEntryList(KTT_STRUCTURES);

        if (holderSet.isEmpty() || holderSet.get().size() == 0) {
            player.sendMessage(Text.translatable(
                    "message.doctor_m.tracer.no_structure_tag",
                    "doctor_m:ktt_fragment_structures"
            ), true);
            return;
        }

        AsyncLocatorUtil.locate(extWorld, KTT_STRUCTURES, searchCenter,
                STRUCTURE_SEARCH_RADIUS, false).thenOnServerThread(result -> {

            if (result == null) {
                consoleWorld.playSound(null, console,
                        SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.BLOCKS, 1f, 0.5f);
                player.sendMessage(Text.translatable(
                        "message.doctor_m.tracer.no_structure_signal",
                        STRUCTURE_SEARCH_RADIUS
                ), true);
                return;
            }

            BlockPos structurePos = result;
            BlockPos dest = structurePos.add(
                    extWorld.random.nextInt(160) - 80,
                    0,
                    extWorld.random.nextInt(160) - 80
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
            player.sendMessage(Text.translatable("message.doctor_m.tracer.structure_locked"), true);
        });
    }
}