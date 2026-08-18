package doctor_m.client.Accessory;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AccessoryPassiveButton {
    private static KeyBinding keySkill; // Z
    private static KeyBinding keyCore;  // X
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        keySkill = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.passive_a",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.doctor_m"
        ));
        keyCore = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.passive_b",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.doctor_m"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (keySkill.wasPressed()) {
                AccessoryKeyRegistry.handleSkillKey(client.player);
            }
            if (keyCore.wasPressed()) {
                AccessoryKeyRegistry.handleCoreKey(client.player);
            }
        });
    }
}