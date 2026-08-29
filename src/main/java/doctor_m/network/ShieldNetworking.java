package doctor_m.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ShieldNetworking {
    public static final Identifier SHIELD_ACTIVATION = new Identifier("doctor_m", "shield_activation");

    public static void sendShieldActivation(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, SHIELD_ACTIVATION, PacketByteBufs.empty());
    }
}