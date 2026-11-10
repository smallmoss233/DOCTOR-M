package doctor_m.client.network;

import doctor_m.network.SARSwitchNetwork;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public class SARSwitchClient {
    private static KeyBinding keyI;
    private static KeyBinding keyU;

    public static void register() {
        keyI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.sar_switch_i",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.doctor_m"
        ));
        keyU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.doctor_m.sar_switch_u",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.doctor_m"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (keyI.wasPressed()) {
                send(SARSwitchNetwork.TARGET_I);
            }
            while (keyU.wasPressed()) {
                send(SARSwitchNetwork.TARGET_U);
            }
        });
    }

    private static void send(byte target) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(target);
        ClientPlayNetworking.send(SARSwitchNetwork.ID, buf);
    }
}