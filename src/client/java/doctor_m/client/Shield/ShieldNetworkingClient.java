package doctor_m.client.Shield;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import doctor_m.network.ShieldNetworking;

@Environment(EnvType.CLIENT)
public class ShieldNetworkingClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                ShieldNetworking.SHIELD_ACTIVATION,
                (client, handler, buf, responseSender) -> {
                    // 关键修复：Netty IO 线程不能操作 SoundManager，必须切回客户端主线程
                    client.execute(() -> ShieldOverlay.triggerShield());
                }
        );
    }
}