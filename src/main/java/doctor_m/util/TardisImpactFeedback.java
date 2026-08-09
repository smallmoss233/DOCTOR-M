package doctor_m.util;

import doctor_m.network.TardisImpactS2CPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;

public class TardisImpactFeedback {

    public static void apply(ServerTardis tardis, Vec3d impactPos, float intensity) {
        if (tardis == null) return;

        if (tardis.travel().getState() != TravelHandlerBase.State.LANDED
                && tardis.travel().getState() != TravelHandlerBase.State.MAT) {
            return;
        }

        ServerWorld interior = tardis.asServer().world();
        if (interior == null) return;

        var consoles = tardis.getDesktop().getConsolePos();
        if (consoles.isEmpty()) return;

        if (intensity >= 0.7f) {
            for (BlockPos console : consoles) {
                spawnHeavyParticles(interior, console);
                interior.playSound(null, console, SoundEvents.ENTITY_GENERIC_EXPLODE,
                        SoundCategory.BLOCKS, 0.6f, 0.8f);
            }
            tardis.alarm().enable();

        } else if (intensity >= 0.3f) {
            for (BlockPos console : consoles) {
                spawnMediumParticles(interior, console);
                interior.playSound(null, console, SoundEvents.BLOCK_ANVIL_LAND,
                        SoundCategory.BLOCKS, 0.5f, 0.6f);
            }

        } else {
            for (BlockPos console : consoles) {
                spawnLightParticles(interior, console);
                interior.playSound(null, console, SoundEvents.BLOCK_METAL_HIT,
                        SoundCategory.BLOCKS, 0.3f, 0.7f);
            }
        }

        // 1.20.1 发送方式
        for (ServerPlayerEntity player : interior.getPlayers()) {
            float shake = Math.min(intensity * 20f, 40f);
            PacketByteBuf buf = PacketByteBufs.create();
            TardisImpactS2CPacket.write(buf, impactPos, shake);
            ServerPlayNetworking.send(player, TardisImpactS2CPacket.ID, buf);
        }
    }

    private static void spawnHeavyParticles(ServerWorld world, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos);
        for (int i = 0; i < 15; i++) {
            double ox = (world.random.nextDouble() - 0.5) * 2.0;
            double oy = (world.random.nextDouble() - 0.5) * 2.0;
            double oz = (world.random.nextDouble() - 0.5) * 2.0;
            world.spawnParticles(ParticleTypes.LAVA, c.x + ox, c.y + oy, c.z + oz, 1, 0, 0, 0, 0.1);
        }
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1, c.z, 8, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticles(ParticleTypes.FLAME, c.x, c.y + 1, c.z, 5, 0.3, 0.3, 0.3, 0.05);
    }

    private static void spawnMediumParticles(ServerWorld world, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos);
        for (int i = 0; i < 8; i++) {
            double ox = (world.random.nextDouble() - 0.5) * 1.5;
            double oy = (world.random.nextDouble() - 0.5) * 1.5;
            double oz = (world.random.nextDouble() - 0.5) * 1.5;
            world.spawnParticles(ParticleTypes.LAVA, c.x + ox, c.y + oy, c.z + oz, 1, 0, 0, 0, 0.05);
        }
        world.spawnParticles(ParticleTypes.SMOKE, c.x, c.y + 1, c.z, 5, 0.3, 0.3, 0.3, 0.03);
    }

    private static void spawnLightParticles(ServerWorld world, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos);
        world.spawnParticles(ParticleTypes.SMOKE, c.x, c.y + 1, c.z, 3, 0.2, 0.2, 0.2, 0.02);
        world.spawnParticles(ParticleTypes.CRIT, c.x, c.y + 1, c.z, 2, 0.2, 0.2, 0.2, 0.1);
    }
}