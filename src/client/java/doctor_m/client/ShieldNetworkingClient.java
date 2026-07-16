package doctor_m.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import doctor_m.world_data.ShieldNetworking;

@Environment(EnvType.CLIENT)
public class ShieldNetworkingClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                ShieldNetworking.SHIELD_ACTIVATION,
                (client, handler, buf, responseSender) -> {
                    // TODO: 触发护盾叠加层渲染
                    ShieldOverlay.triggerShield(); // 必须调用这个！
                }
        );
    }
}