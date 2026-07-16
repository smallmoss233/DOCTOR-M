package doctor_m.world_data.TimeKey

import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_itme.fragment.relic_gem
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

object GemTickHandler {
    @JvmStatic
    fun register() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerManager.playerList) {
                TrinketsApi.getTrinketComponent(player).ifPresent { component ->
                    component.getEquipped { stack -> stack.item is relic_gem }
                        .forEach { pair ->
                            relic_gem.tick(player, pair.right)
                        }
                }
            }
        }
    }
}