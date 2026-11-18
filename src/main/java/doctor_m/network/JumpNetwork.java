package doctor_m.network;

import doctor_m.handler.JumpKitHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class JumpNetwork {
    public static final Identifier ID = new Identifier("doctor_m", "jump_action");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                JumpKitHandler.tryJump(player);
            });
        });
    }
}