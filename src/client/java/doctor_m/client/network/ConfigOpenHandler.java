package doctor_m.client.network;

import doctor_m.client.Config.ConfigScreenBuilder;
import doctor_m.client.Config.DOCTORMConfigScreen;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.network.ConfigOpenPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;

public class ConfigOpenHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigOpenPacket.ID, (client, handler, buf, sender) -> {
            client.execute(() -> {
                if (client.player == null) return;

                ModConfig cfg = ConfigManager.getConfig();
                Screen parent = client.currentScreen;
                DOCTORMConfigScreen screen = ConfigScreenBuilder.build(parent, cfg);
                client.setScreen(screen);
            });
        });
    }
}