package doctor_m.client.network;

import doctor_m.network.ATCLModificationSystemNetwork;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ATCLModificationSystemKeyClient {
    private static KeyBinding keyZ;

    public static void register() {
        keyZ = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.open_atcl_modification_system",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.doctor_m"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (keyZ.wasPressed()) {
                ClientPlayNetworking.send(ATCLModificationSystemNetwork.ID, PacketByteBufs.empty());
            }
        });
    }
}