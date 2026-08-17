package doctor_m.network;

import doctor_m.handler.KeytoTime.KeytoTimeActive;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class KeytoTimeActiveNetwork {
    public static final Identifier ACTIVE_ABILITY = new Identifier("doctor_m", "key_to_time_active");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ACTIVE_ABILITY, (server, player, handler, buf, responseSender) -> {
            int abilityId = buf.readInt();
            server.execute(() -> {
                if (!KeytoTimeCore.isTimeKeyEquipped(player)) return;
                switch (abilityId) {
                    case 0 -> KeytoTimeActive.toggleGameMode(player);
                    case 1 -> KeytoTimeActive.toggleDifficulty(player);
                }
            });
        });
    }
}