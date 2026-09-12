package doctor_m.mixin.stp;

import dev.amble.ait.AITMod;
import dev.amble.ait.api.ExtraPushableEntity;
import dev.amble.ait.api.tardis.TardisEvents;
import dev.amble.ait.core.blockentities.DoorBlockEntity;
import dev.amble.ait.core.entities.FlightTardisEntity;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.util.TardisUtil;
import dev.amble.ait.core.util.WorldUtil;
import dev.amble.lib.data.DirectedBlockPos;
import dev.drtheo.scheduler.api.TimeUnit;
import dev.drtheo.scheduler.api.common.Scheduler;
import dev.drtheo.scheduler.api.common.TaskStage;
import doctor_m.module.STP;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TardisUtil.class)
public class TardisUtilMixin {

    /**
     * 塔迪斯进出传送（外框碰撞、门口走入等）。
     *
     * <p>先等预加载完成（复用门开时启动的 future），再执行 STP 传送。
     * 这样即使玩家秒开门秒碰门，也不会出现加载屏幕。
     */
    @Inject(method = "teleportWithDoorOffset", at = @At("HEAD"), cancellable = true)
    private static void doctor_m$stpTeleportWithDoorOffset(
            ServerWorld world, Entity entity, DirectedBlockPos directed, CallbackInfo ci) {

        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (player.getWorld() == world) return;

        if (!AITMod.CONFIG.tntCanTeleportThroughDoors && entity instanceof TntEntity) return;
        if (entity instanceof ExtraPushableEntity pushable
                && pushable.ait$pushBehaviour() == TriState.FALSE) return;

        BlockPos pos = directed.getPos();
        boolean isDoor = world.getBlockEntity(pos) instanceof DoorBlockEntity;

        Vec3d vec = isDoor
                ? TardisUtil.offsetInteriorDoorPos(directed)
                : TardisUtil.offsetDoorPosition(directed).add(0, 0.125, 0);

        float targetYaw = RotationPropertyHelper.toDegrees(directed.getRotation())
                + (isDoor ? 0f : 180f);

        // 提前标记 push 状态，防止等待预加载期间重复碰门触发多次
        if (entity instanceof ExtraPushableEntity pushable)
            pushable.ait$setPushBehaviour(TriState.FALSE);

        ChunkPos chunk = new ChunkPos(
                (int) Math.floor(vec.x) >> 4,
                (int) Math.floor(vec.z) >> 4);

        // ★ 等预加载完成后再传送
        STP.preloadAllAsync(player, world, chunk).thenRun(() -> {
            world.getServer().execute(() -> {
                if (player.isDisconnected()) return;
                if (entity.getVehicle() instanceof FlightTardisEntity) return;

                try {
                    STP.teleport(player, world, vec, targetYaw, player.getPitch());
                    doctor_m$syncPostTeleport(player);
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                    STP.LOGGER.debug("Tardis STP (door): {} -> {} at {}",
                            player.getName().getString(),
                            world.getRegistryKey().getValue(), vec);
                } catch (Throwable t) {
                    STP.LOGGER.warn("Tardis STP failed, falling back to vanilla", t);
                    WorldUtil.teleportToWorld(player, world, vec, targetYaw, player.getPitch());
                }

                if (entity instanceof ExtraPushableEntity pushable)
                    Scheduler.get().runTaskLater(
                            () -> pushable.ait$setPushBehaviour(TriState.DEFAULT),
                            TaskStage.END_SERVER_TICK, TimeUnit.SECONDS, 3);
            });
        });

        ci.cancel();
    }

    /**
     * Sonic 遥控进塔迪斯。
     */
    @Inject(method = "teleportToInteriorPosition", at = @At("HEAD"), cancellable = true)
    private static void doctor_m$stpTeleportToInteriorPosition(
            ServerTardis tardis, Entity entity, BlockPos pos, CallbackInfo ci) {

        if (!(entity instanceof ServerPlayerEntity player)) return;

        ServerWorld targetWorld = tardis.world();
        if (targetWorld == null) return;
        if (player.getWorld() == targetWorld) return;

        if (TardisEvents.ENTER_TARDIS.invoker().onEnter(tardis, entity)
                == TardisEvents.Interaction.FAIL) return;

        Vec3d vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        float yaw = entity.getYaw();

        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);

        // ★ 等预加载完成后再传送
        STP.preloadAllAsync(player, targetWorld, chunk).thenRun(() -> {
            targetWorld.getServer().execute(() -> {
                if (player.isDisconnected()) return;

                try {
                    STP.teleport(player, targetWorld, vec, yaw, player.getPitch());
                    doctor_m$syncPostTeleport(player);
                    player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

                    STP.LOGGER.debug("Tardis STP (interior): {} -> {} at {}",
                            player.getName().getString(),
                            targetWorld.getRegistryKey().getValue(), vec);
                } catch (Throwable t) {
                    STP.LOGGER.warn("Tardis STP (interior) failed, falling back", t);
                    WorldUtil.teleportToWorld(player, targetWorld, vec, yaw, player.getPitch());
                }
            });
        });

        ci.cancel();
    }

    private static void doctor_m$syncPostTeleport(ServerPlayerEntity player) {
        player.addExperience(0);
        player.getStatusEffects().forEach(effect ->
                player.networkHandler.sendPacket(
                        new EntityStatusEffectS2CPacket(player.getId(), effect)));
    }
}