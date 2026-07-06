package world_data;

import doctor_m.Item.data_itme.fragment.mystery_gem;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class GemTickHandler {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                    component.getEquipped(stack -> stack.getItem() instanceof mystery_gem).forEach(pair -> {
                        mystery_gem.tick(player, pair.getRight());
                    });
                });
            }
        });
    }
}