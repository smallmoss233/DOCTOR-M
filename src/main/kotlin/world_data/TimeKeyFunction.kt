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

    // ========== 公共 API（供 Java Mixin 调用） ==========
    @JvmStatic
    fun getTimeKeyStack(player: PlayerEntity): ItemStack =
        player.mainHandStack.takeIf { it.item is time_key }
            ?: TrinketsApi.getTrinketComponent(player)
                .flatMap { it.getEquipped { stack -> stack.item is time_key }.stream().findFirst() }
                .map { it.right }
                .orElse(ItemStack.EMPTY)

    @JvmStatic
    fun isTimeKeyEquipped(player: PlayerEntity): Boolean =
        getTimeKeyStack(player).isEmpty == false

    @JvmStatic
    fun isGodMode(player: PlayerEntity): Boolean =
        isTimeKeyEquipped(player) && getTimeKeyStack(player).orCreateNbt.getBoolean("godmode")

    @JvmStatic
    fun register() {
        // ===== 1. 伤害拦截（所有装备者） =====
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            customDamage.get().also { if (it) customDamage.set(false) }
            (entity as? ServerPlayerEntity)?.let { player ->
                if (!isTimeKeyEquipped(player)) return@register true

                // GodMode：完全免疫，直接满血
                if (isGodMode(player)) {
                    player.health = player.maxHealth
                    return@register false
                }

                // 弹开箭矢（所有装备者）
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

                // 常规环境伤害免疫（所有装备者）
                when (source.name) {
                    "inFire", "onFire", "lava", "magic", "indirectMagic", "wither", "drown",
                    "starve", "fall", "cactus", "hotFloor", "sweetBerryBush", "freeze",
                    "inWall", "lightningBolt", "thorns", "sonic_boom", "outOfWorld",
                    "dryout", "stalagmite", "fallingStalactite", "cramming", "flyIntoWall",
                    "generic" -> return@register false
                }

                // 伤害上限 15% + 致死复活（非 GodMode）
                val maxAllowed = player.maxHealth * 0.15f
                val newAmount = amount.coerceAtMost(maxAllowed)
                val newHealth = player.health - newAmount
                if (newHealth <= 0) {
                    revivePlayer(player)
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

        // ===== ★ 新增：死亡事件拦截（所有装备者） =====
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            (entity as? ServerPlayerEntity)?.let { player ->
                if (isTimeKeyEquipped(player)) {
                    revivePlayer(player)
                    return@register false
                }
            }
            true
        }

        // ===== 2. 命令系统 =====
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            fun handle(player: PlayerEntity, key: String, msg: String) {
                getTimeKeyStack(player).takeIf { it.item is time_key }?.orCreateNbt?.apply {
                    putBoolean(key, !getBoolean(key))
                    player.sendMessage(Text.translatable("$msg.${if (getBoolean(key)) "on" else "off"}"), true)
                }
            }
            dispatcher.register(CommandManager.literal("passive")
                .then(CommandManager.literal("a").executes { handle(it.source.player!!, "neutral_mode", "message.doctor_m.time_key.neutral_mode"); 1 })
                .then(CommandManager.literal("b").executes { handle(it.source.player!!, "godmode", "message.doctor_m.time_key.godmode"); 1 })
            )
        }

        // ===== 3. Tick 循环：恢复 + 灭火 + 飞行 + 有氧 + 绝对保护 =====
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.ticks.toLong()
            server.playerManager.playerList.forEach { player ->
                val hasTimeKey = isTimeKeyEquipped(player)
                val isGodMode = hasTimeKey && isGodMode(player)

                // ------ 基础恢复（所有装备者） ------
                if (hasTimeKey) {
                    val healAmount = player.maxHealth * 0.1f
                    lastHealTime[player.uuid]?.let { last ->
                        if (now - last >= 20) {
                            player.heal(healAmount)
                            lastHealTime[player.uuid] = now
                        }
                    } ?: run {
                        player.heal(healAmount)
                        lastHealTime[player.uuid] = now
                    }

                    val hunger = player.hungerManager
                    val foodAdd = healAmount.toInt()
                    val newFood = (hunger.foodLevel + foodAdd).coerceAtMost(20)
                    val newSaturation = (hunger.saturationLevel + healAmount).coerceAtMost(newFood.toFloat())
                    hunger.foodLevel = newFood
                    hunger.saturationLevel = newSaturation

                    if (player.isOnFire) {
                        player.fireTicks = 0
                        player.setOnFire(false)
                    }
                }

                // ------ ★ GodMode 绝对不死循环（额外层） ------
                if (isGodMode) {
                    if (player.health < player.maxHealth) player.health = player.maxHealth
                    if (player.isOnFire) {
                        player.fireTicks = 0
                        player.setOnFire(false)
                    }
                    player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE)
                    player.removeStatusEffect(StatusEffects.WITHER)
                    if (player.air < player.maxAir) player.air = player.maxAir
                    if (player.hungerManager.foodLevel < 20) {
                        player.hungerManager.foodLevel = 20
                        player.hungerManager.saturationLevel = 20f
                    }
                    // 极端边缘：已死或血量为 0，强制完整复活
                    if (!player.isAlive || player.health <= 0) {
                        revivePlayer(player)
                    }
                }

                // ------ 飞行恢复 ------
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

                // ------ 永久有氧 ------
                if (hasTimeKey) {
                    val world = player.world
                    val planet = try { PlanetRegistry.getInstance().get(world) } catch (_: Exception) { null }
                    val worldHasOxygen = if (planet != null) planet.hasOxygen() else true

                    if (!worldHasOxygen) {
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

    // ====== 复活核心（所有装备者共用） ======
    @JvmStatic
    fun revivePlayer(p: ServerPlayerEntity) {
        p.apply {
            // 反射彻底重置死亡状态字段
            try {
                val clazz = LivingEntity::class.java
                clazz.getDeclaredField("deathTime").apply {
                    isAccessible = true
                    set(this@apply, 0)
                }
                clazz.getDeclaredField("hurtTime").apply {
                    isAccessible = true
                    set(this@apply, 0)
                }
                clazz.getDeclaredField("fallDistance").apply {
                    isAccessible = true
                    set(this@apply, 0f)
                }
            } catch (_: Exception) { }

            // 强制满血
            health = maxHealth
            // 清除所有负面效果
            clearStatusEffects()
            // 给抗性
            addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false))
            // 清敌
            serverWorld.getEntitiesByClass(LivingEntity::class.java, boundingBox.expand(35.0)) {
                it != this && it.isAlive && it is HostileEntity
            }.forEach { it.kill() }
            // 粒子特效
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