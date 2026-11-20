package doctor_m.network;

import doctor_m.handler.JumpKitHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class JumpNetwork {
    public static final Identifier ID = new Identifier("doctor_m", "jump_action");
    public static final Identifier SLIDING_ID = new Identifier("doctor_m", "sliding_state");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                JumpKitHandler.tryJump(player);
            });
        });
    }

    public static void sendSlidingState(ServerPlayerEntity player, boolean isSliding) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isSliding);
        ServerPlayNetworking.send(player, SLIDING_ID, buf);
    }
}