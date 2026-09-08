package doctor_m.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class INVERTSCREENPACKETNetwork {
    public static final Identifier INVERT_SCREEN_PACKET = new Identifier("doctor_m", "invert_screen");

    public static void sendInvertScreenPacket(ServerPlayerEntity player, int durationTicks) {
        var buf = PacketByteBufs.create();
        buf.writeInt(durationTicks);
        ServerPlayNetworking.send(player, INVERT_SCREEN_PACKET, buf);
    }
}