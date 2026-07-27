package doctor_m.world_data.TimeKey

import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_itme.fragment.RelicGemItem
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GemDeathSaveHandler {
    // 存储玩家无敌结束的游戏刻
    private val invincibleMap = ConcurrentHashMap<UUID, Long>()

    @JvmStatic
    fun register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            if (entity !is ServerPlayerEntity) return@register true

            val player = entity
            val world = player.world
            val currentTick = world.time

            // ====== 1. 优先检查无敌状态 ======
            val endTick = invincibleMap[player.uuid]
            if (endTick != null && currentTick < endTick) {
                // 无敌时间内，取消本次伤害
                return@register false
            } else if (endTick != null) {
                // 已过期，清理
                invincibleMap.remove(player.uuid)
            }

            // ====== 2. 检测致命伤害 ======
            if (player.health - amount > 0) return@register true

            // ====== 3. 查找装备的宝石 ======
            val gemStack = TrinketsApi.getTrinketComponent(player)
                .orElse(null)
                ?.getEquipped { stack -> stack.item is RelicGemItem }
                ?.firstOrNull()
                ?.right ?: return@register true

            // ====== 4. 检查冷却 ======
            val cooldownUntil = RelicGemItem.getCooldownUntilTick(gemStack)
            if (currentTick < cooldownUntil) return@register true

            // ====== 5. 触发复活 ======
            val level = RelicGemItem.getLevel(gemStack)
            val activeTicks = RelicGemItem.getActiveTicks(level)
            val cooldownTicks = RelicGemItem.getCooldownTicks(level)
            val totalTicks = activeTicks + cooldownTicks

            // 恢复满血
            player.health = player.maxHealth

            // 移除被动抗性（避免干扰）
            player.removeStatusEffect(StatusEffects.RESISTANCE)

            // 添加主动增益（速度、夜视、急迫、水下呼吸），不包含抗性
            val effects = listOf(
                StatusEffectInstance(StatusEffects.SPEED, activeTicks, 2, false, true, true),
                StatusEffectInstance(StatusEffects.NIGHT_VISION, activeTicks, 0, false, true, true),
                StatusEffectInstance(StatusEffects.HASTE, activeTicks, 1, false, true, true),
                StatusEffectInstance(StatusEffects.WATER_BREATHING, activeTicks, 0, false, true, true)
            )
            effects.forEach { player.addStatusEffect(it) }

            // 设置无敌时间（从当前刻开始，持续 activeTicks 刻）
            invincibleMap[player.uuid] = currentTick + activeTicks

            // 设置冷却（游戏刻）
            RelicGemItem.setCooldownUntilTick(gemStack, currentTick + totalTicks)

            // 特效 & 音效
            repeat(60) {
                val x = player.x + (player.random.nextDouble() - 0.5) * 2.0
                val y = player.y + player.random.nextDouble() * 2.5
                val z = player.z + (player.random.nextDouble() - 0.5) * 2.0
                player.serverWorld.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.2)
            }
            player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0f, 1.0f)

            // 取消本次致命伤害
            return@register false
        }
    }
}