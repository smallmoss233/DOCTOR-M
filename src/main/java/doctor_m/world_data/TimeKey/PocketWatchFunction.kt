package doctor_m.world_data.TimeKey

import com.ibm.icu.impl.Pair
import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_itme.fragment.pocket_watch
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text

object PocketWatchFunction {
    const val COOLDOWN_KEY = "table_revival_cooldown_end_ms"
    private const val COOLDOWN_MILLIS = 24000L * 50L // 1 游戏日

    @JvmStatic
    fun register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            if (entity !is ServerPlayerEntity) return@register true

            val player = entity
            val watchStack = findPocketWatch(player)
            if (watchStack == null) return@register true

            val nbt = watchStack.orCreateNbt
            val currentTime = System.currentTimeMillis()
            val cooldownEnd = nbt.getLong(COOLDOWN_KEY)

            // 冷却中 → 正常受伤，不弹提示
            if (currentTime < cooldownEnd) {
                return@register true
            }

            val newHealth = player.health - amount
            if (newHealth <= 0) {
                revivePlayer(player)
                nbt.putLong(COOLDOWN_KEY, System.currentTimeMillis() + COOLDOWN_MILLIS)
                return@register false
            }
            true
        }
    }

    @JvmStatic
    fun getRemainingTimeParts(millis: Long): kotlin.Pair<Int, Int> {
        val totalSeconds = millis / 1000
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return kotlin.Pair(minutes, seconds)
    }

    // 查找背包中的 pocket_watch（主手、副手、背包、饰品）
    private fun findPocketWatch(player: ServerPlayerEntity): ItemStack? {
        for (stack in player.inventory.main) {
            if (stack.item is pocket_watch) return stack
        }
        if (player.inventory.offHand.firstOrNull { it.item is pocket_watch } != null) {
            return player.inventory.offHand.first { it.item is pocket_watch }
        }
        val trinketOpt = TrinketsApi.getTrinketComponent(player)
            .flatMap { comp -> comp.getEquipped { stack -> stack.item is pocket_watch }.stream().findFirst() }
        if (trinketOpt.isPresent) {
            return trinketOpt.get().right
        }
        return null
    }

    private fun revivePlayer(player: ServerPlayerEntity) {
        player.health = player.maxHealth / 2f
        player.clearStatusEffects()

        val radius = 10.0
        player.serverWorld.getEntitiesByClass(
            LivingEntity::class.java,
            player.boundingBox.expand(radius)
        ) { it != player && it.isAlive }
            .forEach { it.damage(player.damageSources.magic(), 25.0f) }

        player.addStatusEffect(StatusEffectInstance(StatusEffects.HUNGER, 60, 3, false, false))
        player.addStatusEffect(StatusEffectInstance(StatusEffects.DARKNESS, 300, 1, false, false))

        repeat(40) {
            val x = player.x + (player.random.nextDouble() - 0.5) * 1.5
            val y = player.y + player.random.nextDouble() * 2.0
            val z = player.z + (player.random.nextDouble() - 0.5) * 1.5
            player.serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.0, 0.0, 0.0, 0.1)
        }
        player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0f, 1.0f)
        player.sendMessage(Text.translatable("message.doctor_m.pocket_watch.revived"), true)
    }
}