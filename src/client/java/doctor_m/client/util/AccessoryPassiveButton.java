package doctor_m.client.util;

import doctor_m.Item.stcs.STCSItem;
import doctor_m.client.gui.KeytoTimeActiveScreen;
import doctor_m.client.gui.KeytoTimePassiveScreen;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.network.STCSNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AccessoryPassiveButton implements ClientModInitializer {
    private static KeyBinding keySkill;   // Z
    private static KeyBinding keyCore;  // X

    @Override
    public void onInitializeClient() {
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

            // ===== 优先级1：主手 STCS =====
            ItemStack main = client.player.getMainHandStack();
            if (main.getItem() instanceof STCSItem) {
                if (keySkill.wasPressed()) {
                    ClientPlayNetworking.send(STCSNetworking.STCS_SKILL_ID, PacketByteBufs.create());
                }
                if (keyCore.wasPressed()) {
                    ClientPlayNetworking.send(STCSNetworking.STCS_CORE_ID, PacketByteBufs.create());
                }
                return; // 主手有 STCS，不再往下检测
            }

            // ===== 优先级2：副手 STCS =====
            ItemStack off = client.player.getOffHandStack();
            if (off.getItem() instanceof STCSItem) {
                if (keySkill.wasPressed()) {
                    ClientPlayNetworking.send(STCSNetworking.STCS_SKILL_ID, PacketByteBufs.create());
                }
                if (keyCore.wasPressed()) {
                    ClientPlayNetworking.send(STCSNetworking.STCS_CORE_ID, PacketByteBufs.create());
                }
                return; // 副手有 STCS，不再往下检测
            }

            // ===== 优先级3：TimeKey（主手/副手/饰品栏任一位置） =====
            boolean hasTimeKey = KeytoTimeCore.isTimeKeyEquipped(client.player);

            if (keySkill.wasPressed() && hasTimeKey) {
                client.setScreen(new KeytoTimePassiveScreen(client.player));
            }
            if (keyCore.wasPressed() && hasTimeKey) {
                client.setScreen(new KeytoTimeActiveScreen(client.player));
            }
        });
    }
}