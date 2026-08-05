package doctor_m.handler.TimeKey

import dev.amble.ait.core.AITStatusEffects
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry
import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_itme.TimeKeyItem
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
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.world.GameMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object TimeKeyFunction {
    private val customDamage = ThreadLocal.withInitial { false }
    private val lastGameMode = ConcurrentHashMap<UUID, GameMode>()
    private val lastHealTime = ConcurrentHashMap<UUID, Long>()

    @JvmStatic
    fun getTimeKeyStack(player: PlayerEntity): ItemStack =
        player.mainHandStack.takeIf { it.item is TimeKeyItem }
            ?: TrinketsApi.getTrinketComponent(player)
                .flatMap { it.getEquipped { stack -> stack.item is TimeKeyItem }.stream().findFirst() }
                .map { it.right }
                .orElse(ItemStack.EMPTY)

    @JvmStatic
    fun isTimeKeyEquipped(player: PlayerEntity): Boolean =
        getTimeKeyStack(player).isEmpty == false

    @JvmStatic
    fun register() {
        // ===== 1. 伤害拦截 =====
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            customDamage.get().also { if (it) customDamage.set(false) }
            (entity as? ServerPlayerEntity)?.let { player ->
                if (!isTimeKeyEquipped(player)) return@register true

                if (TimeKeyPassive.isGodMode(player)) {
                    player.health = player.maxHealth
                    return@register false
                }

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

                when (source.name) {
                    "inFire", "onFire", "lava", "magic", "indirectMagic", "wither", "drown",
                    "starve", "fall", "cactus", "hotFloor", "sweetBerryBush", "freeze",
                    "inWall", "lightningBolt", "thorns", "sonic_boom", "outOfWorld",
                    "dryout", "stalagmite", "fallingStalactite", "cramming", "flyIntoWall",
                    "generic" -> return@register false
                }

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

        // ===== 2. 死亡拦截 =====
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            (entity as? ServerPlayerEntity)?.let { player ->
                if (isTimeKeyEquipped(player)) {
                    revivePlayer(player)
                    return@register false
                }
            }
            true
        }

        // ===== 3. Tick 循环 =====
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.ticks.toLong()
            server.playerManager.playerList.forEach { player ->
                val hasTimeKey = isTimeKeyEquipped(player)
                val isGodMode = hasTimeKey && TimeKeyPassive.isGodMode(player)

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
                    if (!player.isAlive || player.health <= 0) {
                        revivePlayer(player)
                    }
                }

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
        TimeKeyPassive.registerAttackCallback()
    }

    @JvmStatic
    fun revivePlayer(p: ServerPlayerEntity) {
        p.apply {
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

            health = maxHealth
            clearStatusEffects()
            addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false))
            serverWorld.getEntitiesByClass(LivingEntity::class.java, boundingBox.expand(35.0)) {
                it != this && it.isAlive && it is HostileEntity
            }.forEach { it.kill() }
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