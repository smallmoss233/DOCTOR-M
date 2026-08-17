package doctor_m.handler.KeytoTime

import doctor_m.util.creativity.ScytheSlashManager
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult

object KeytoTimePassive {
    private const val GODMODE_KEY = "godmode"
    private const val NEUTRAL_KEY = "neutral_mode"
    private const val SLASH_KEY = "slash_mode"

    @JvmStatic
    fun toggleFeature(player: PlayerEntity, featureId: Int) {
        val stack = KeytoTimeCore.getTimeKeyStack(player)
        if (stack.isEmpty) return
        val nbt = stack.orCreateNbt
        val key = when (featureId) {
            0 -> GODMODE_KEY
            1 -> NEUTRAL_KEY
            2 -> SLASH_KEY
            else -> return
        }
        val current = nbt.getBoolean(key)
        nbt.putBoolean(key, !current)

        val msgKey = when (featureId) {
            0 -> "gui.doctor_m.key_to_time.godmode"
            1 -> "gui.doctor_m.key_to_time.neutral_mode"
            2 -> "gui.doctor_m.key_to_time.slash_mode"
            else -> return
        }
        player.sendMessage(Text.translatable("$msgKey.${if (!current) "on" else "off"}"), true)
    }

    @JvmStatic
    fun isGodMode(player: PlayerEntity): Boolean {
        val stack = KeytoTimeCore.getTimeKeyStack(player)
        return !stack.isEmpty && stack.orCreateNbt.getBoolean(GODMODE_KEY)
    }

    @JvmStatic
    fun isNeutralMode(player: PlayerEntity): Boolean {
        val stack = KeytoTimeCore.getTimeKeyStack(player)
        return !stack.isEmpty && stack.orCreateNbt.getBoolean(NEUTRAL_KEY)
    }

    @JvmStatic
    fun isSlashMode(player: PlayerEntity): Boolean {
        val stack = KeytoTimeCore.getTimeKeyStack(player)
        return !stack.isEmpty && stack.orCreateNbt.getBoolean(SLASH_KEY)
    }

    @JvmStatic
    fun registerAttackCallback() {
        AttackEntityCallback.EVENT.register { player, world, hand, entity, hitResult ->
            if (world.isClient) return@register ActionResult.PASS
            if (player !is ServerPlayerEntity) return@register ActionResult.PASS
            if (entity !is LivingEntity) return@register ActionResult.PASS
            if (!KeytoTimeCore.isTimeKeyEquipped(player)) return@register ActionResult.PASS
            if (!isSlashMode(player)) return@register ActionResult.PASS

            ScytheSlashManager.performSlashEffect(player.serverWorld, player, 5)
            ActionResult.PASS
        }
    }
}