package doctor_m.handler.KeytoTime;

import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.SlotReference;
import doctor_m.Item.data_item.KeytoTimeFragment.RelicGemItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.Optional;

public class GemTickHandler {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Optional<TrinketComponent> componentOptional = TrinketsApi.getTrinketComponent(player);
                if (componentOptional.isPresent()) {
                    TrinketComponent component = componentOptional.get();
                    List<Pair<SlotReference, ItemStack>> equipped = component.getEquipped(stack -> stack.getItem() instanceof RelicGemItem);
                    for (Pair<SlotReference, ItemStack> pair : equipped) {
                        RelicGemItem.tick(player, pair.getRight());
                    }
                }
            }
        });
    }
}