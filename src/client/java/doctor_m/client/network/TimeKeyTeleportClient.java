package doctor_m.client.network;

import doctor_m.client.gui.TimeKeyTeleportScreen;
import doctor_m.network.TimeKeyTeleportNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public class TimeKeyTeleportClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TimeKeyTeleportNetwork.DIMS_RESPONSE, (client, handler, buf, responseSender) -> {
            int count = buf.readInt();
            List<String> dims = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                dims.add(buf.readString());
            }
            client.execute(() -> {
                if (client.currentScreen instanceof TimeKeyTeleportScreen screen) {
                    screen.onDimensionsReceived(dims);
                }
            });
        });
    }
}