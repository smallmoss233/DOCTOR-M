package doctor_m.handler.KeytoTime

import dev.amble.ait.core.AITStatusEffects
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry
import dev.emi.trinkets.api.TrinketsApi
import doctor_m.Item.data_item.KeytoTimeItem
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.world.GameMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object KeytoTimeCore {
    private val customDamage = ThreadLocal.withInitial { false }
    private val lastGameMode = ConcurrentHashMap<UUID, GameMode>()
    private val lastHealTime = ConcurrentHashMap<UUID, Long>()
    private val protectionEndTime = ConcurrentHashMap<UUID, Long>()

    private val VANILLA_DAMAGE_TYPES = setOf(
        "inFire", "onFire", "lava", "hotFloor", "inWall", "cramming",
        "drown", "starve", "cactus", "fall", "flyIntoWall", "outOfWorld",
        "generic", "magic", "indirectMagic", "dragonBreath", "wither",
        "anvil", "fallingStalactite", "stalagmite", "lightningBolt", "freeze",
        "sonicBoom", "outsideBorder", "genericKill", "dryout", "sweetBerryBush",
        "fallingBlock", "trident", "arrow", "mob", "player", "explosion",
        "fireworks", "fireball", "witherSkull", "thrown", "sting", "badRespawnPoint"
    )

    private fun ensureMaxHealth(player: ServerPlayerEntity) {
        val attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)
        if (attr != null && attr.value <= 0.0) {
            HealthGuard.withMaxHealthWrite {
                attr.baseValue = 20.0
            }
        }
    }

    @JvmStatic
    fun getTimeKeyStack(player: PlayerEntity): ItemStack {
        return try {
            getTimeKeyStackUnsafe(player)
        } catch (e: NullPointerException) {
            ItemStack.EMPTY
        }
    }

    private fun getTimeKeyStackUnsafe(player: PlayerEntity): ItemStack {
        // 主手
        player.mainHandStack.takeIf { it.item is KeytoTimeItem }?.let { return it }
        // 副手
        player.offHandStack.takeIf { it.item is KeytoTimeItem }?.let { return it }

        // 背包（含装备栏、副手槽）
        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            if (stack.item is KeytoTimeItem) return stack
        }

        // 末影箱
        val enderChest = player.enderChestInventory
        if (enderChest != null) {
            for (i in 0 until enderChest.size()) {
                val stack = enderChest.getStack(i)
                if (stack.item is KeytoTimeItem) return stack
            }
        }

        // Trinkets 饰品栏
        return try {
            TrinketsApi.getTrinketComponent(player)
                .flatMap { it.getEquipped { stack -> stack.item is KeytoTimeItem }.stream().findFirst() }
                .map { it.right }
                .orElse(ItemStack.EMPTY)
        } catch (_: Throwable) {
            ItemStack.EMPTY
        }
    }

    @JvmStatic
    fun isTimeKeyEquipped(player: PlayerEntity): Boolean =
        getTimeKeyStack(player).isEmpty == false

    @JvmStatic
    fun onDeathIntercepted(player: ServerPlayerEntity) {
        protectionEndTime[player.uuid] = player.server.ticks + 2400L
    }

    @JvmStatic
    fun clearProtection(player: ServerPlayerEntity) {
        protectionEndTime.remove(player.uuid)
    }

    @JvmStatic
    fun register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            if (customDamage.get()) {
                customDamage.set(false)
                return@register true
            }

            (entity as? ServerPlayerEntity)?.let { player ->
                if (!isTimeKeyEquipped(player)) return@register true

                ensureMaxHealth(player)
                if (KeytoTimePassive.isGodMode(player)) {
                    HealthGuard.withHealthWrite {
                        player.health = player.maxHealth
                    }
                    return@register false
                }

                if (source.name !in VANILLA_DAMAGE_TYPES) {
                    return@register false
                }

                protectionEndTime[player.uuid]?.let { end ->
                    if (player.server.ticks <= end) {
                        ensureMaxHealth(player)
                        HealthGuard.withHealthWrite {
                            player.health = player.maxHealth
                        }
                        return@register false
                    }
                }

                (source.source as? ProjectileEntity)?.takeIf { it.owner != player }?.let { proj ->
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
                    "inWall", "lightningBolt", "thorns", "sonicBoom", "outOfWorld",
                    "dryout", "stalagmite", "fallingStalactite", "cramming", "flyIntoWall",
                    "generic" -> return@register false
                }

                val maxAllowed = player.maxHealth * 0.15f
                val newAmount = amount.coerceAtMost(maxAllowed)
                val newHealth = player.health - newAmount

                if (newHealth <= 0) {
                    ensureMaxHealth(player)
                    HealthGuard.withHealthWrite {
                        player.health = player.maxHealth
                    }
                    onDeathIntercepted(player)
                    return@register false
                }

                if (newAmount != amount) {
                    customDamage.set(true)
                    HealthGuard.withHealthWrite {
                        player.damage(source, newAmount)
                    }
                    return@register false
                }
            }
            true
        }

        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            (entity as? ServerPlayerEntity)?.let { player ->
                if (isTimeKeyEquipped(player)) {
                    ensureMaxHealth(player)
                    revivePlayer(player)
                    return@register false
                }
            }
            true
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.ticks.toLong()
            server.playerManager.playerList.forEach { player ->
                val hasTimeKey = isTimeKeyEquipped(player)
                val isGodMode = hasTimeKey && KeytoTimePassive.isGodMode(player)

                protectionEndTime[player.uuid]?.let { endTick ->
                    if (now <= endTick) {
                        player.serverWorld.getEntitiesByClass(
                            LivingEntity::class.java,
                            player.boundingBox.expand(3.0),
                            { it != player && it.isAlive }
                        ).forEach { target ->
                            eraseTargetDeMatStyle(target, player, player.serverWorld)
                        }
                    } else {
                        protectionEndTime.remove(player.uuid)
                    }
                }

                if (hasTimeKey) {
                    val healAmount = player.maxHealth * 0.1f
                    lastHealTime[player.uuid]?.let { last ->
                        if (now - last >= 20) {
                            HealthGuard.withHealthWrite {
                                player.heal(healAmount)
                            }
                            lastHealTime[player.uuid] = now
                        }
                    } ?: run {
                        HealthGuard.withHealthWrite {
                            player.heal(healAmount)
                        }
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
                    ensureMaxHealth(player)
                    if (player.health < player.maxHealth) {
                        HealthGuard.withHealthWrite {
                            player.health = player.maxHealth
                        }
                    }
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
                        ensureMaxHealth(player)
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
        KeytoTimePassive.registerAttackCallback()
    }

    private fun eraseTargetDeMatStyle(target: LivingEntity, player: ServerPlayerEntity, world: ServerWorld) {
        val pos = target.pos

        repeat(20) {
            world.spawnParticles(ParticleTypes.END_ROD,
                pos.x, pos.y + 0.5, pos.z, 1,
                (world.random.nextDouble() - 0.5) * 0.8,
                (world.random.nextDouble() - 0.5) * 0.8,
                (world.random.nextDouble() - 0.5) * 0.8, 0.05)
        }
        repeat(10) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.x, pos.y + 0.3, pos.z, 1,
                (world.random.nextDouble() - 0.5) * 0.5,
                world.random.nextDouble() * 0.5,
                (world.random.nextDouble() - 0.5) * 0.5, 0.02)
        }

        world.playSound(null, pos.x, pos.y, pos.z,
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.25f, 1.8f)

        if (target is ServerPlayerEntity) {
            target.kill()
        } else {
            target.health = 1f
            val source = player.damageSources.playerAttack(player)
            val died = target.damage(source, 999f)

            if (!died && target.isAlive) {
                target.health = 0f
                try {
                    val onDeath = LivingEntity::class.java.getDeclaredMethod("onDeath", DamageSource::class.java)
                    onDeath.isAccessible = true
                    onDeath.invoke(target, source)
                } catch (_: Exception) {
                    target.kill()
                }
            }
        }
    }

    @JvmStatic
    fun revivePlayer(p: ServerPlayerEntity) {
        if (p.world == null) return

        ensureMaxHealth(p)

        onDeathIntercepted(p)

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

            HealthGuard.withHealthWrite {
                health = maxHealth
            }
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
            sendMessage(Text.translatable("message.doctor_m.key_to_time_resurrection"), true)
            if (!abilities.allowFlying) {
                abilities.allowFlying = true
                sendAbilitiesUpdate()
            }
        }
    }
}