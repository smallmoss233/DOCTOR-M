package doctor_m.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class TimeKeyClient implements ClientModInitializer {
    private static KeyBinding keyBinding;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.toggle_godmode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.doctor_m"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && keyBinding.wasPressed()) {
                if (client.player.isSneaking()) {
                    client.player.networkHandler.sendCommand("timekey godmode");
                }
            }
        });
    }
}