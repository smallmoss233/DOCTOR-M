package doctor_m.handler;

import doctor_m.Item.data_item.JumpKitItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JumpKitHandler {
    private static final UUID SLIDE_SPEED_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    public static final class Config {
        public static double slideSpeedBonus = 1.0;
        public static int slideDurationTicks =10;
        public static int wallClingMaxTicks = 400;
        public static int wallJumpCooldownTicks = 15;
        public static int wallClingGraceTicks = 5;
        public static double wallJumpHorizontalPush = 0.5;
        public static double wallClingEndForwardPush = 0.7;
        public static double wallClingEndUpwardPush = 0.35;
        public static double jumpVelocity = 0.42;
        public static double wallDetectDistance = 0.9;
        public static double wallClingTangentSpeed = 0.3;
        public static double wallClingDescendSpeed = -0.05;
        public static long jumpCooldownMs = 300;
        public static double wallNormalMatchDot = 0.99;
    }

    public static class KitState {
        public boolean hasDoubleJumped = false;
        public int wallClingTicks = 0;
        public int wallClingGraceTicks = 0;
        public int slideTicks = 0;
        public boolean isWallClinging = false;
        public boolean isSliding = false;
        public boolean jumpedDuringSlide = false;
        public boolean slideLocked = false;
        public long lastJumpTime = 0;

        public Vec3d activeWallNormal = null;
        public Vec3d activeWallTangent = null;
        public int wallJumpCooldown = 0;
    }

    private static final Map<UUID, KitState> STATES = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Set<UUID> online = new HashSet<>();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                online.add(player.getUuid());

                if (player.isRemoved() || player.isDead()) {
                    clearState(player);
                    continue;
                }
                if (!hasJumpKit(player)) {
                    clearState(player);
                    continue;
                }

                player.fallDistance = 0.0f;

                KitState state = STATES.computeIfAbsent(player.getUuid(), k -> new KitState());

                if (state.wallJumpCooldown > 0) {
                    state.wallJumpCooldown--;
                }

                handleSlide(player, state);
                handleWallCling(player, state);
            }
            STATES.keySet().removeIf(uuid -> !online.contains(uuid));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player != null) {
                STATES.remove(handler.player.getUuid());
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                clearState(player);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                STATES.remove(newPlayer.getUuid()));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                clearState(player));
    }

    private static void handleSlide(ServerPlayerEntity player, KitState state) {
        if (!player.isSneaking()) {
            state.slideLocked = false;
        }

        if (player.isSneaking() && player.isOnGround() && player.isSprinting()
                && !state.isSliding && state.slideTicks <= 0 && !state.slideLocked) {
            state.isSliding = true;
            state.slideTicks = Config.slideDurationTicks;
            state.jumpedDuringSlide = false;
        }

        if (state.isSliding) {
            if (state.slideTicks <= 0) {
                state.isSliding = false;
                state.slideTicks = 0;
                state.jumpedDuringSlide = false;
                state.slideLocked = true;
                EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speedAttr != null) speedAttr.removeModifier(SLIDE_SPEED_UUID);
                return;
            }

            if (!player.isSneaking()) {
                state.isSliding = false;
                state.slideTicks = 0;
                state.jumpedDuringSlide = false;
                EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speedAttr != null) speedAttr.removeModifier(SLIDE_SPEED_UUID);
                return;
            }

            if (!player.isOnGround()) {
                state.jumpedDuringSlide = true;
            }
            if (state.jumpedDuringSlide && player.isOnGround()) {
                state.isSliding = false;
                state.slideTicks = 0;
                state.jumpedDuringSlide = false;
                state.slideLocked = true;
                EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speedAttr != null) speedAttr.removeModifier(SLIDE_SPEED_UUID);
                return;
            }

            state.slideTicks--;

            if (player.isOnGround()) {
                player.setPose(EntityPose.SWIMMING);
            }

            double progress = state.slideTicks / (double) Math.max(1, Config.slideDurationTicks);
            double speedBonus = Config.slideSpeedBonus * (progress * progress);

            EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                if (speedAttr.getModifier(SLIDE_SPEED_UUID) != null) {
                    speedAttr.removeModifier(SLIDE_SPEED_UUID);
                }

                double multiplier = ((1.0 + speedBonus) / 0.3) - 1.0;

                speedAttr.addTemporaryModifier(new EntityAttributeModifier(
                        SLIDE_SPEED_UUID, "slide_speed", multiplier,
                        EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        } else if (player.getPose() == EntityPose.SWIMMING && !player.isSwimming()) {
            if (player.isSneaking()) {
                player.setPose(EntityPose.CROUCHING);
            } else {
                player.setPose(EntityPose.STANDING);
            }
        }
    }

    private static void handleWallCling(ServerPlayerEntity player, KitState state) {
        if (state.wallJumpCooldown > 0) {
            if (state.isWallClinging) {
                player.setNoGravity(false);
                state.isWallClinging = false;
            }
            state.activeWallNormal = null;
            state.activeWallTangent = null;
            state.wallClingGraceTicks = 0;
            return;
        }

        if (state.isWallClinging && state.wallClingTicks >= Config.wallClingMaxTicks) {
            player.setNoGravity(false);
            state.isWallClinging = false;
            state.wallClingTicks = 0;
            state.wallClingGraceTicks = 0;
            state.hasDoubleJumped = false;
            state.activeWallNormal = null;
            state.activeWallTangent = null;
            player.setVelocity(player.getVelocity().x, -0.5, player.getVelocity().z);
            player.velocityModified = true;
            return;
        }

        boolean inAir = !player.isOnGround() && !player.isTouchingWater() && !player.isInLava();

        Vec3d foundNormal = inAir ? findSurroundingWall(player, state.activeWallNormal) : null;
        boolean touchingWall = foundNormal != null;

        if (touchingWall) {
            if (state.activeWallNormal == null
                    || state.activeWallNormal.dotProduct(foundNormal) < Config.wallNormalMatchDot) {
                state.activeWallNormal = foundNormal;
                Vec3d tangent = foundNormal.crossProduct(new Vec3d(0, 1, 0)).normalize();
                Vec3d lookHorizontal = new Vec3d(
                        player.getRotationVec(1.0F).x, 0, player.getRotationVec(1.0F).z).normalize();
                if (lookHorizontal.dotProduct(tangent) < 0) {
                    tangent = tangent.multiply(-1);
                }
                state.activeWallTangent = tangent;
            }

            state.isWallClinging = true;
            state.wallClingTicks++;
            state.wallClingGraceTicks = 0;

            player.setNoGravity(true);
            player.fallDistance = 0;

            if (player.isSneaking()) {
                player.setVelocity(0, Config.wallClingDescendSpeed, 0);
            } else {
                player.setVelocity(
                        state.activeWallTangent.x * Config.wallClingTangentSpeed,
                        0.0,
                        state.activeWallTangent.z * Config.wallClingTangentSpeed);
            }
            player.velocityModified = true;

            state.hasDoubleJumped = false;
            return;
        }

        if (state.isWallClinging) {
            state.isWallClinging = false;

            if (inAir && state.activeWallNormal != null) {
                if (state.activeWallTangent != null) {
                    Vec3d forwardDir = state.activeWallTangent;
                    player.setVelocity(
                            forwardDir.x * Config.wallClingEndForwardPush,
                            Config.wallClingEndUpwardPush,
                            forwardDir.z * Config.wallClingEndForwardPush
                    );
                    player.velocityModified = true;
                    state.wallJumpCooldown = Config.wallJumpCooldownTicks;
                    state.hasDoubleJumped = false;
                }
                state.wallClingGraceTicks = Config.wallClingGraceTicks;
                player.setNoGravity(false);
            } else {
                player.setNoGravity(false);
                state.activeWallNormal = null;
                state.activeWallTangent = null;
                state.wallClingGraceTicks = 0;
            }
        } else if (state.wallClingGraceTicks > 0) {
            if (!inAir) {
                player.setNoGravity(false);
                state.wallClingGraceTicks = 0;
                state.activeWallNormal = null;
                state.activeWallTangent = null;
            } else {
                state.wallClingGraceTicks--;
                if (state.wallClingGraceTicks <= 0) {
                    state.activeWallNormal = null;
                    state.activeWallTangent = null;
                }
            }
        } else {
            state.activeWallNormal = null;
            state.activeWallTangent = null;
            if (player.isOnGround()) {
                state.hasDoubleJumped = false;
                state.wallClingTicks = 0;
            }
        }
    }

    public static void tryJump(ServerPlayerEntity player) {
        if (!hasJumpKit(player)) return;
        if (player.isTouchingWater() || player.isInLava()) return;

        KitState state = STATES.computeIfAbsent(player.getUuid(), k -> new KitState());

        long now = System.currentTimeMillis();
        if (now - state.lastJumpTime < Config.jumpCooldownMs) return;

        if (state.wallJumpCooldown > 0) {
            if (!state.hasDoubleJumped) {
                Vec3d vel = player.getVelocity();
                player.setVelocity(vel.x, Config.jumpVelocity, vel.z);
                player.velocityModified = true;
                state.hasDoubleJumped = true;
                state.lastJumpTime = now;
            }
            return;
        }

        Vec3d currentWallNormal = (!player.isOnGround() && !player.isTouchingWater() && !player.isInLava())
                ? findSurroundingWall(player, state.activeWallNormal)
                : null;

        boolean canWallJump = currentWallNormal != null
                || state.isWallClinging
                || (state.wallClingGraceTicks > 0 && state.activeWallNormal != null);

        if (canWallJump) {
            player.setNoGravity(false);
            Vec3d normalToUse = currentWallNormal != null ? currentWallNormal : state.activeWallNormal;
            if (normalToUse == null) normalToUse = player.getRotationVec(1.0F).multiply(-1);

            player.setVelocity(
                    normalToUse.x * Config.wallJumpHorizontalPush,
                    Config.jumpVelocity,
                    normalToUse.z * Config.wallJumpHorizontalPush);
            player.velocityModified = true;

            state.isWallClinging = false;
            state.wallClingTicks = 0;
            state.wallClingGraceTicks = 0;
            state.lastJumpTime = now;
            state.hasDoubleJumped = false;
            state.activeWallNormal = null;
            state.activeWallTangent = null;
            state.wallJumpCooldown = Config.wallJumpCooldownTicks;
            return;
        }

        if (!state.hasDoubleJumped) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x, Config.jumpVelocity, vel.z);
            player.velocityModified = true;
            state.hasDoubleJumped = true;
            state.lastJumpTime = now;
        }
    }

    private static Vec3d findSurroundingWall(PlayerEntity player, Vec3d preferNormal) {
        Vec3d eyePos = player.getPos().add(0, player.getStandingEyeHeight(), 0);
        double distance = Config.wallDetectDistance;
        float yaw = player.getYaw();

        Vec3d forward = new Vec3d(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        Vec3d right = new Vec3d(-Math.cos(Math.toRadians(yaw)), 0, -Math.sin(Math.toRadians(yaw)));

        List<Vec3d> hits = new ArrayList<>(4);
        if (raycastBlock(player, eyePos, forward.multiply(distance))) hits.add(forward.multiply(-1).normalize());
        if (raycastBlock(player, eyePos, forward.multiply(-distance))) hits.add(forward.normalize());
        if (raycastBlock(player, eyePos, right.multiply(distance))) hits.add(right.multiply(-1).normalize());
        if (raycastBlock(player, eyePos, right.multiply(-distance))) hits.add(right.normalize());

        if (hits.isEmpty()) return null;
        if (preferNormal == null) return hits.get(0);

        Vec3d best = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (Vec3d n : hits) {
            double d = n.dotProduct(preferNormal);
            if (d > bestDot) {
                bestDot = d;
                best = n;
            }
        }
        return best;
    }

    private static boolean raycastBlock(PlayerEntity player, Vec3d start, Vec3d dir) {
        HitResult result = player.getWorld().raycast(new RaycastContext(start, start.add(dir),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return result.getType() == HitResult.Type.BLOCK;
    }

    private static void clearState(ServerPlayerEntity player) {
        KitState state = STATES.remove(player.getUuid());
        if (state == null) return;

        state.isSliding = false;
        state.isWallClinging = false;
        state.jumpedDuringSlide = false;
        state.slideLocked = false;
        state.slideTicks = 0;
        state.wallClingTicks = 0;
        state.wallClingGraceTicks = 0;
        state.lastJumpTime = 0;
        state.wallJumpCooldown = 0;
        state.activeWallNormal = null;
        state.activeWallTangent = null;

        player.setNoGravity(false);
        if (player.getPose() == EntityPose.SWIMMING && !player.isSwimming()) {
            player.setPose(EntityPose.STANDING);
        }

        EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SLIDE_SPEED_UUID);
        }
    }

    public static boolean hasJumpKit(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(comp -> comp.isEquipped((stack) -> stack.getItem() instanceof JumpKitItem))
                .orElse(false);
    }

    public static boolean hasJumpKitClient(PlayerEntity player) {
        return hasJumpKit(player);
    }
}