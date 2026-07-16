package doctor_m.world_data

import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_itme.fragment.relic_gem
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents

object GemDeathSaveHandler {
@JvmStatic
fun register() {
    ServerPlayerEvents.ALLOW_DEATH.register { player, damageSource, damageAmount ->
            var saved = false

        TrinketsApi.getTrinketComponent(player).ifPresent { component ->
                component.getEquipped { stack -> stack.item is relic_gem }.forEach { pair ->
                val gemStack = pair.right
            if (relic_gem.tryTriggerDeathSave(player, gemStack)) {
                saved = true
                player.health = 1.0f
                player.clearStatusEffects()
                // 重新触发一次确保效果上满（因为 clearStatusEffects 清掉了）
                relic_gem.tryTriggerDeathSave(player, gemStack)
            }
        }
        }

        // 返回 false 阻止死亡（saved = true），true 允许死亡
        !saved
    }
}
}