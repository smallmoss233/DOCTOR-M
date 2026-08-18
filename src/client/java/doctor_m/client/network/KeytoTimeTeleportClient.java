package doctor_m.client.network;

import doctor_m.client.gui.KeytoTimeTeleportScreen;
import doctor_m.network.KeytoTimeTeleportNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public class KeytoTimeTeleportClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(KeytoTimeTeleportNetwork.DIMS_RESPONSE, (client, handler, buf, responseSender) -> {
            int count = buf.readInt();
            List<String> dims = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                dims.add(buf.readString());
            }
            client.execute(() -> {
                if (client.currentScreen instanceof KeytoTimeTeleportScreen screen) {
                    screen.onDimensionsReceived(dims);
                }
            });
        });
    }
}