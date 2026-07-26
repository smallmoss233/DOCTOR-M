package doctor_m.world_data.TimeKey

import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_itme.fragment.relic_gem
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents

object GemDeathSaveHandler {
    private const val COOLDOWN_KEY = "gem_revival_cooldown_end_ms"

    @JvmStatic
    fun register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            if (entity !is ServerPlayerEntity) return@register true

            val player = entity
            // 不致命，正常受伤
            if (player.health - amount > 0) return@register true

            // 查找宝石（参考 PocketWatchFunction 的查找方式）
            val gemStack = TrinketsApi.getTrinketComponent(player)
                .map { comp -> comp.getEquipped { stack -> stack.item is relic_gem }.firstOrNull()?.right }
                .orElse(null) ?: return@register true

            val nbt = gemStack.orCreateNbt
            val currentTime = System.currentTimeMillis()
            val cooldownEnd = nbt.getLong(COOLDOWN_KEY)

            // 冷却中 → 正常受伤，不触发
            if (currentTime < cooldownEnd) return@register true

            // === 触发复活 ===
            val level = relic_gem.getLevel(gemStack)
            val activeTicks = relic_gem.getActiveTicks(level)
            val cooldownTicks = relic_gem.getCooldownTicks(level)
            val totalTicks = activeTicks + cooldownTicks

            // 1. 恢复生命并清空所有效果（与 PocketWatchFunction 一致）
            player.health = 1.0f
            player.clearStatusEffects()

            // 2. 添加主动 BUFF（抗性5、速度3、夜视、急迫2、水下呼吸）
            val effects = listOf(
                StatusEffectInstance(StatusEffects.RESISTANCE, activeTicks, 4, false, true, true),
                StatusEffectInstance(StatusEffects.SPEED, activeTicks, 2, false, true, true),
                StatusEffectInstance(StatusEffects.NIGHT_VISION, activeTicks, 0, false, true, true),
                StatusEffectInstance(StatusEffects.HASTE, activeTicks, 1, false, true, true),
                StatusEffectInstance(StatusEffects.WATER_BREATHING, activeTicks, 0, false, true, true)
            )
            effects.forEach { player.addStatusEffect(it) }

            // 3. 设置冷却（毫秒）
            nbt.putLong(COOLDOWN_KEY, currentTime + totalTicks * 50L)

            // 4. 同步设置游戏刻冷却，防止被动抗性提前覆盖（双重保障）
            relic_gem.setCooldownUntil(gemStack, player.world.time + totalTicks)

            // 5. 特效 & 提示（和 PocketWatchFunction 风格一致）
            repeat(60) {
                val x = player.x + (player.random.nextDouble() - 0.5) * 2.0
                val y = player.y + player.random.nextDouble() * 2.5
                val z = player.z + (player.random.nextDouble() - 0.5) * 2.0
                player.serverWorld.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.2)
            }
            player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0f, 1.0f)

            // 6. 取消本次伤害
            false
        }
    }
}