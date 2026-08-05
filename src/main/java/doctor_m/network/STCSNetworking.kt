package doctor_m.network

import doctor_m.Item.stcs.STCSItem
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.Identifier

object STCSNetworking {
    @JvmField
    val STCS_SKILL_ID = Identifier("doctor_m", "stcs_skill")

    @JvmField
    val STCS_CORE_ID = Identifier("doctor_m", "stcs_core")

    @JvmStatic
    fun register() {
        // Z 键：二技能
        ServerPlayNetworking.registerGlobalReceiver(STCS_SKILL_ID) { server, player, _, _, _ ->
            server.execute {
                var stack = player.mainHandStack
                if (stack.item !is STCSItem) stack = player.offHandStack
                (stack.item as? STCSItem)?.onSkillPressed(player, stack)
            }
        }

        // X 键：剑核心 开/关
        ServerPlayNetworking.registerGlobalReceiver(STCS_CORE_ID) { server, player, _, _, _ ->
            server.execute {
                var stack = player.mainHandStack
                if (stack.item !is STCSItem) stack = player.offHandStack
                (stack.item as? STCSItem)?.onCorePressed(player, stack)
            }
        }
    }
}