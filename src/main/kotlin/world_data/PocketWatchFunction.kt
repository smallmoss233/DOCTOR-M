package world_data

import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import doctor_m.Item.data_itme.fragment.pocket_watch
import net.minecraft.entity.LivingEntity

object PocketWatchFunction {
    const val COOLDOWN_KEY = "table_revival_cooldown_end_ms"
    private const val COOLDOWN_MILLIS = 24000L * 50L // 1 游戏日 = 1,200,000 毫秒

    @JvmStatic
    fun register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            if (entity !is ServerPlayerEntity) return@register true

            val tableOpt = TrinketsApi.getTrinketComponent(entity)
                .flatMap { comp -> comp.getEquipped { stack -> stack.item is pocket_watch }.stream().findFirst() }

            if (tableOpt.isEmpty) return@register true

            val tableStack = tableOpt.get().right
            val nbt = tableStack.orCreateNbt
            val currentTime = System.currentTimeMillis()
            val cooldownEnd = nbt.getLong(COOLDOWN_KEY)

            // 冷却中 → 无法复活
            if (currentTime < cooldownEnd) {
                return@register true
            }

            val newHealth = entity.health - amount
            if (newHealth <= 0) {
                revivePlayer(entity)
                nbt.putLong(COOLDOWN_KEY, System.currentTimeMillis() + COOLDOWN_MILLIS)
                return@register false
            }
            true
        }
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

    @JvmStatic
    fun formatRemainingTime(millis: Long): String {
        val seconds = millis / 1000
        if (seconds < 60) return "${seconds}秒"
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        if (minutes < 60) return "${minutes}分${remainingSeconds}秒"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return "${hours}小时${remainingMinutes}分"
    }
}