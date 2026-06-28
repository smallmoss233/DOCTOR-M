package world_data

import dev.amble.ait.core.AITStatusEffects
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry
import dev.emi.trinkets.api.TrinketsApi
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.command.CommandManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.world.GameMode
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import doctor_m.Item.data_itme.time_key
import java.lang.Boolean.getBoolean

object TimeKeyFunction {
    // 状态缓存
    private val customDamage = ThreadLocal.withInitial { false }
    private val revivalCooldown = ConcurrentHashMap<UUID, Long>()
    private val lastGameMode = ConcurrentHashMap<UUID, GameMode>()
    private val lastHealTime = ConcurrentHashMap<UUID, Long>()
    private const val COOLDOWN_TICKS = 200L

    @JvmStatic
    fun register() {
        // 1. 伤害处理
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            customDamage.get().also { if (it) customDamage.set(false) }
            (entity as? ServerPlayerEntity)?.let { player ->
                if (!isTimeKeyEquipped(player)) return@register true
                // 弹开箭矢
                (source.source as? PersistentProjectileEntity)?.takeIf { it.owner != player }?.let { proj ->
                    player.world.playSound(null, player.x, player.y, player.z,
                        SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.5f, 1.5f)
                    repeat(10) {
                        player.serverWorld.spawnParticles(ParticleTypes.CLOUD,
                            player.x + (player.random.nextDouble() - .5),
                            player.y + player.random.nextDouble(),
                            player.z + (player.random.nextDouble() - .5),
                            1, 0.0, 0.0, 0.0, 0.0)
                    }
                    proj.discard()
                    return@register false
                }
                // 完全免疫模式
                val stack = getTimeKeyStack(player)
                if (stack.orCreateNbt.getBoolean("godmode")) return@register false
                // 常规伤害免疫
                when (source.name) {
                    "inFire", "onFire", "lava", "magic", "indirectMagic", "wither", "drown",
                    "starve", "fall", "cactus", "hotFloor", "sweetBerryBush", "freeze",
                    "inWall", "lightningBolt", "thorns", "sonic_boom", "outOfWorld",
                    "dryout", "stalagmite", "fallingStalactite", "cramming", "flyIntoWall",
                    "generic" -> return@register false
                }
                // 伤害上限与复活
                val maxAllowed = player.maxHealth * 0.15f
                val newAmount = amount.coerceAtMost(maxAllowed)
                val newHealth = player.health - newAmount
                if (newHealth <= 0 && !isInCooldown(player)) {
                    revivePlayer(player)
                    revivalCooldown[player.uuid] = player.serverWorld.time + COOLDOWN_TICKS
                    return@register false
                }
                if (newAmount != amount) {
                    customDamage.set(true)
                    player.damage(source, newAmount)
                    return@register false
                }
            }
            true
        }

        // 2. 命令系统（极简）
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            fun handle(player: PlayerEntity, key: String, msg: String) {
                getTimeKeyStack(player).takeIf { it.item is time_key }?.orCreateNbt?.apply {
                    putBoolean(key, !getBoolean(key))
                    player.sendMessage(Text.translatable("$msg.${if (getBoolean(key)) "on" else "off"}"), true)
                } ?: player.sendMessage(Text.translatable("message.doctor_m.time_key.not_equipped"), true)
            }
            dispatcher.register(CommandManager.literal("passive")
                .then(CommandManager.literal("a").executes { handle(it.source.player!!, "neutral_mode", "message.doctor_m.time_key.neutral_mode"); 1 })
                .then(CommandManager.literal("b").executes { handle(it.source.player!!, "godmode", "message.doctor_m.time_key.godmode"); 1 })
            )
        }

        // 合并：生命恢复 + 饱食度 + 灭火 + 飞行恢复 + 永久有氧（仅无氧星球）
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.ticks.toLong()
            server.playerManager.playerList.forEach { player ->
                val hasTimeKey = isTimeKeyEquipped(player)

                // ====== 1. 生命恢复 + 饱食度 + 灭火 ======
                if (hasTimeKey) {
                    val healAmount = player.maxHealth * 0.1f
                    // 恢复生命值（每秒）
                    lastHealTime[player.uuid]?.let { last ->
                        if (now - last >= 20) {
                            player.heal(healAmount)
                            lastHealTime[player.uuid] = now
                        }
                    } ?: run {
                        player.heal(healAmount)
                        lastHealTime[player.uuid] = now
                    }
                    // 恢复饱食度
                    val hunger = player.hungerManager
                    val foodAdd = healAmount.toInt()
                    val newFood = (hunger.foodLevel + foodAdd).coerceAtMost(20)
                    val newSaturation = (hunger.saturationLevel + healAmount).coerceAtMost(newFood.toFloat())
                    hunger.foodLevel = newFood
                    hunger.saturationLevel = newSaturation
                    // 灭火
                    if (player.isOnFire) {
                        player.fireTicks = 0
                        player.setOnFire(false)
                    }
                }

                // ====== 2. 飞行恢复（模式切换检测） ======
                val current = player.interactionManager.gameMode
                lastGameMode[player.uuid]?.takeIf { it != current }?.let { previous ->
                    if ((previous == GameMode.CREATIVE || previous == GameMode.SPECTATOR) &&
                        (current == GameMode.SURVIVAL || current == GameMode.ADVENTURE) &&
                        hasTimeKey && !player.abilities.allowFlying
                    ) {
                        player.abilities.allowFlying = true
                        player.sendAbilitiesUpdate()
                    }
                }
                lastGameMode[player.uuid] = current

                // ====== 3. 永久有氧（仅无氧星球） ======
                if (hasTimeKey) {
                    val world = player.world
                    // 检测当前世界是否为无氧环境（星球检测：若无星球数据，默认为有氧）
                    val planet = try { PlanetRegistry.getInstance().get(world) } catch (_: Exception) { null }
                    val worldHasOxygen = if (planet != null) planet.hasOxygen() else true // 默认有氧

                    if (!worldHasOxygen) {
                        // 无氧环境：施加 OXYGENATED 效果
                        if (!player.hasStatusEffect(AITStatusEffects.OXYGENATED)) {
                            player.addStatusEffect(
                                StatusEffectInstance(AITStatusEffects.OXYGENATED, 60, 0, false, false)
                            )
                        } else {
                            val effect = player.getStatusEffect(AITStatusEffects.OXYGENATED)
                            if (effect != null && effect.duration < 40) {
                                player.addStatusEffect(
                                    StatusEffectInstance(AITStatusEffects.OXYGENATED, 60, 0, false, false)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ====== 工具函数 ======
    private fun getTimeKeyStack(player: PlayerEntity) =
        player.mainHandStack.takeIf { it.item is time_key }
            ?: TrinketsApi.getTrinketComponent(player)
                .flatMap { it.getEquipped { stack -> stack.item is time_key }.stream().findFirst() }
                .map { it.right }
                .orElse(ItemStack.EMPTY)

    private fun isTimeKeyEquipped(player: PlayerEntity) =
        TrinketsApi.getTrinketComponent(player)
            .map { it.isEquipped { stack -> stack.item is time_key } }
            .orElse(false)

    private fun isInCooldown(player: ServerPlayerEntity) =
        revivalCooldown[player.uuid]?.let { player.serverWorld.time < it } ?: false

    private fun revivePlayer(p: ServerPlayerEntity) {
        p.apply {
            health = maxHealth
            clearStatusEffects()
            addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false))
            // 清敌
            serverWorld.getEntitiesByClass(LivingEntity::class.java, boundingBox.expand(35.0)) {
                it != this && it.isAlive && it is HostileEntity
            }.forEach { it.kill() }
            // 粒子
            repeat(50) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                    x + (random.nextDouble() - .5) * 2.0,
                    y + random.nextDouble() * 2.0,
                    z + (random.nextDouble() - .5) * 2.0,
                    1, 0.0, 0.0, 0.0, 0.1)
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    x + (random.nextDouble() - .5) * 2.0,
                    y + random.nextDouble() * 2.0,
                    z + (random.nextDouble() - .5) * 2.0,
                    1, 0.0, 0.0, 0.0, 0.05)
            }
            playSound(SoundEvents.BLOCK_BELL_RESONATE, 1f, 1f)
            sendMessage(Text.translatable("message.doctor_m.time_key_resurrection"), true)
            if (!abilities.allowFlying) {
                abilities.allowFlying = true
                sendAbilitiesUpdate()
            }
        }
    }
}