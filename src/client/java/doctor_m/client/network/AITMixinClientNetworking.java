package doctor_m.client.network;

import doctor_m.mixin.PlayerEntityAccessor;
import doctor_m.network.TardisImpactS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AITMixinClientNetworking {
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TardisImpactS2CPacket.ID,
                (client, handler, buf, responseSender) -> {
                    float shake = TardisImpactS2CPacket.read(buf);

                    client.execute(() -> {
                        if (client.player != null) {
                            ((PlayerEntityAccessor) client.player)
                                    .aitmixin$setDamageTiltYaw(shake
                                            * (client.world.random.nextBoolean() ? 1 : -1));
                        }
                    });
                });
    }
}