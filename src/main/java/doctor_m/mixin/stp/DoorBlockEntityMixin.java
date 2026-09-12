package doctor_m.mixin.stp;

import dev.amble.ait.core.blockentities.DoorBlockEntity;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.lib.data.DirectedBlockPos;
import doctor_m.module.STP;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 塔迪斯门打开时异步预加载对面维度的区块。
 * 配置禁用 STP 时不做任何事。
 */
@Mixin(DoorBlockEntity.class)
public class DoorBlockEntityMixin {

    @Unique
    private static final Map<UUID, Boolean> doctor_m$lastDoorState = new ConcurrentHashMap<>();

    @Unique
    private static final double doctor_m$PRELOAD_DISTANCE_SQ = 256.0;

    @Inject(method = "tick", at = @At("HEAD"))
    private static <T extends BlockEntity> void doctor_m$preloadOnDoorOpen(
            World world, BlockPos pos, BlockState blockState, T tDoor, CallbackInfo ci) {

        // ★ 配置禁用时直接返回
        if (!STP.isEnabled()) return;

        if (!(world instanceof ServerWorld)) return;
        if (!(tDoor instanceof DoorBlockEntity door)) return;
        if (!door.isLinked()) return;

        Tardis tardis = door.tardis().get();
        if (tardis == null) return;

        boolean open = tardis.door().isOpen();
        Boolean previous = doctor_m$lastDoorState.put(tardis.getUuid(), open);

        // 只在门从关→开时触发
        if (!open || Boolean.TRUE.equals(previous)) return;

        doctor_m$preloadBothSides(tardis);
    }

    @Unique
    private static void doctor_m$preloadBothSides(Tardis tardis) {
        if (!(tardis instanceof ServerTardis serverTardis)) return;

        ServerWorld interiorWorld = serverTardis.world();
        ServerWorld exteriorWorld = tardis.travel().position().getWorld();
        BlockPos exteriorPos = tardis.travel().position().getPos();
        DirectedBlockPos doorPos = tardis.getDesktop().getDoorPos();

        if (interiorWorld == null || exteriorWorld == null || doorPos == null) return;

        ChunkPos interiorChunk = new ChunkPos(doorPos.getPos());
        ChunkPos exteriorChunk = new ChunkPos(exteriorPos);

        try {
            // 内部玩家 → 预加载外世界（异步）
            for (ServerPlayerEntity player : interiorWorld.getPlayers()) {
                STP.preloadAll(player, exteriorWorld, exteriorChunk);
            }

            // 外部附近玩家 → 预加载塔迪斯内部（异步）
            for (ServerPlayerEntity player : exteriorWorld.getPlayers()) {
                if (player.squaredDistanceTo(
                        exteriorPos.getX(), exteriorPos.getY(), exteriorPos.getZ())
                        < doctor_m$PRELOAD_DISTANCE_SQ) {
                    STP.preloadAll(player, interiorWorld, interiorChunk);
                }
            }

            STP.LOGGER.debug("Tardis door opened: triggered async preload for {}",
                    tardis.getUuid());
        } catch (Throwable t) {
            STP.LOGGER.warn("Tardis door preload failed", t);
        }
    }
}