package doctor_m.network;

import doctor_m.Item.data_item.KeytoTimeItem;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.handler.KeytoTime.KeytoTimePassive;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class KeytoTimeNetwork {
    public static final Identifier TOGGLE_PASSIVE = new Identifier("doctor_m", "key_to_time_toggle_passive");
    public static final Identifier SET_TITLE = new Identifier("doctor_m", "key_to_time_set_title");

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PASSIVE, (server, player, handler, buf, responseSender) -> {
            int feature = buf.readInt();
            server.execute(() -> KeytoTimePassive.toggleFeature(player, feature));
        });

        ServerPlayNetworking.registerGlobalReceiver(SET_TITLE, (server, player, handler, buf, responseSender) -> {
            String title = buf.readString(64);
            server.execute(() -> {
                var stack = KeytoTimeCore.getTimeKeyStack(player);
                if (!stack.isEmpty() && stack.getItem() instanceof KeytoTimeItem) {
                    KeytoTimeItem.setTitle(stack, title);
                }
            });
        });
    }
}