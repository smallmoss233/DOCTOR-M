package doctor_m.client.util;

import doctor_m.client.gui.TimeKeyActiveScreen;
import doctor_m.client.gui.TimeKeyPassiveScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AccessoryPassiveButton implements ClientModInitializer {
    private static KeyBinding keyPassiveA;
    private static KeyBinding keyPassiveB;

    @Override
    public void onInitializeClient() {
        keyPassiveA = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.toggle_passive_a",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.doctor_m"
        ));
        keyPassiveB = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.toggle_passive_b",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.doctor_m"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // 直接开 UI，不再发指令
            if (keyPassiveA.wasPressed()) {
                client.setScreen(new TimeKeyPassiveScreen(client.player));
            }
            if (keyPassiveB.wasPressed()) {
                client.setScreen(new TimeKeyActiveScreen(client.player));
            }
        });
    }
}