package doctor_m.network;

import doctor_m.handler.TimeKey.TimeKeyPassive;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class TimeKeyNetwork {
    public static final Identifier TOGGLE_PASSIVE = new Identifier("doctor_m", "time_key_toggle_passive");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PASSIVE, (server, player, handler, buf, responseSender) -> {
            int feature = buf.readInt();
            server.execute(() -> TimeKeyPassive.toggleFeature(player, feature));
        });
    }
}