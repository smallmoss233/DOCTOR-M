package doctor_m.handler.KeytoTime

import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_item.KeytoTimeFragment.RelicGemItem
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

object GemTickHandler {
    @JvmStatic
    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerManager.playerList) {
                TrinketsApi.getTrinketComponent(player).ifPresent { component ->
                    component.getEquipped { stack -> stack.item is RelicGemItem }
                        .forEach { pair ->
                            RelicGemItem.tick(player, pair.right)
                        }
                }
            }
        }
    }
}