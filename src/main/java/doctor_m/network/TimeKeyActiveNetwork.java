package doctor_m.network;

import doctor_m.handler.TimeKey.TimeKeyActive;
import doctor_m.handler.TimeKey.TimeKeyFunction;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class TimeKeyActiveNetwork {
    public static final Identifier ACTIVE_ABILITY = new Identifier("doctor_m", "time_key_active");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ACTIVE_ABILITY, (server, player, handler, buf, responseSender) -> {
            int abilityId = buf.readInt();
            server.execute(() -> {
                if (!TimeKeyFunction.isTimeKeyEquipped(player)) return;
                switch (abilityId) {
                    case 0 -> TimeKeyActive.toggleGameMode(player);
                    case 1 -> TimeKeyActive.toggleDifficulty(player);
                }
            });
        });
    }
}