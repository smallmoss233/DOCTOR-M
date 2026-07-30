package doctor_m.entities.data

import doctor_m.trading.TradeManager
import doctor_m.trading.TradeOffer
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.*
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.world.World
import java.util.*

class Entity103wEvereye(entityType: EntityType<out PathAwareEntity>, world: World) : PathAwareEntity(entityType, world) {

    enum class AIState { IDLE, TRADING, COMBAT, RETALIATING }

    companion object {
        val CURRENT_STATE: TrackedData<Int> = DataTracker.registerData(
            Entity103wEvereye::class.java, TrackedDataHandlerRegistry.INTEGER
        )

        // ==================== 本地化键名常量 ====================
        private const val BASE = "entity.doctor_m.103w_evereye"

        val STAGE1_KEYS      = (0..4).map { "$BASE.dialog.stage1.$it" }
        val STAGE2_KEYS      = (0..4).map { "$BASE.dialog.stage2.$it" }
        val STAGE3_KEYS      = (0..4).map { "$BASE.dialog.stage3.$it" }
        val PEACEFUL_KEYS    = (0..4).map { "$BASE.dialog.peaceful.$it" }
        val ANGRY_KEYS       = (0..4).map { "$BASE.dialog.angry.$it" }
        val HURT_KEYS        = (0..4).map { "$BASE.dialog.hurt.$it" }

        fun createMobAttributes(): DefaultAttributeContainer.Builder =
            PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
    }

    // ==================== 反击与记忆系统 ====================
    private var lastRetaliateTime = 0L
    private var isAngry = false
    private var angerTimer = 0
    private var lastAggressorUUID: UUID? = null
    private var lastAggressionTime = 0L

    private var lastDamageTime = 0L

    private var aggressionCount = 0
    private var hasWarnedCurrentAggressor = false

    private val RETALIATE_COOLDOWN = 20
    private val AGGRESSION_MEMORY = 48000L   // 2 游戏日 = 2 * 24000 ticks
    private val ANGER_DURATION = 7200        // 6 分钟 = 6 * 60 * 20 ticks

    // ==================== 交易系统 ====================
    private var dailyTrades: MutableList<TradeOffer> = ArrayList()
    private var lastTradeRefreshDay = -1L
    private val TRADE_POOL_FILE = "evereye_trade.json"

    override fun initGoals() {
        super.initGoals()
        goalSelector.add(0, SwimGoal(this))
        goalSelector.add(1, FleeEntityGoal(this, HostileEntity::class.java, 10.0f, 0.8, 0.4))
        goalSelector.add(2, LookAtEntityGoal(this, PlayerEntity::class.java, 12.0f))
        goalSelector.add(3, LookAroundGoal(this))
        goalSelector.add(4, WanderAroundGoal(this, 0.25))
        goalSelector.add(5, WanderAroundFarGoal(this, 0.4))
        targetSelector.add(1, RevengeGoal(this, PlayerEntity::class.java))
    }

    override fun tick() {
        super.tick()
        if (!world.isClient && isAngry) {
            if (angerTimer > 0) {
                angerTimer--
            } else {
                calmDown()
            }
        }

        if (!world.isClient && world is ServerWorld) {
            val sw = world as ServerWorld
            val currentDay = sw.time / 24000L
            if (currentDay > lastTradeRefreshDay) {
                refreshTrades(sw.server)
            }
        }

        if (!world.isClient && health < maxHealth) {
            val now = world.time
            if (now - lastDamageTime > 100 && age % 40 == 0) {
                heal(1.0f)
            }
        }
    }

    private fun calmDown() {
        isAngry = false
        hasWarnedCurrentAggressor = false
        aggressionCount = 0
        setAttacker(null)
        setTarget(null)
        if (!world.isClient) setState(AIState.IDLE)
    }

    private fun refreshTrades(server: MinecraftServer) {
        val pool = TradeManager.loadPoolFromDatapack(server, TRADE_POOL_FILE)
        dailyTrades = TradeManager.generateDailyTrades(pool, random)
        lastTradeRefreshDay = server.overworld.time / 24000L
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        val damaged = super.damage(source, amount)
        if (!damaged || world.isClient) return damaged

        lastDamageTime = world.time

        if (isDead || health <= 0.0f) return damaged

        val attacker = source.attacker as? LivingEntity ?: return damaged
        val now = world.time
        val attackerId = attacker.uuid

        val isNewAggression = lastAggressorUUID == null
                || lastAggressorUUID != attackerId
                || (now - lastAggressionTime) > AGGRESSION_MEMORY

        if (isNewAggression) {
            lastAggressorUUID = attackerId
            lastAggressionTime = now
            hasWarnedCurrentAggressor = false
            aggressionCount = 1

            if (now - lastRetaliateTime >= RETALIATE_COOLDOWN) {
                lastRetaliateTime = now
                handleAggressionStage(attacker, aggressionCount)
            }

            if (random.nextFloat() < 0.3f && attacker is ServerPlayerEntity) {
                attacker.sendMessage(Text.translatable(HURT_KEYS.random()), true)
            }
            return damaged
        }

        if (now - lastRetaliateTime < RETALIATE_COOLDOWN) return damaged
        lastRetaliateTime = now
        lastAggressionTime = now
        aggressionCount++

        handleAggressionStage(attacker, aggressionCount)
        return damaged
    }

    private fun handleAggressionStage(attacker: LivingEntity, stage: Int) {
        if (!isAngry) {
            isAngry = true
            angerTimer = ANGER_DURATION
            setTarget(attacker)
            setState(AIState.COMBAT)
        } else {
            angerTimer = ANGER_DURATION
        }

        when {
            stage == 1 -> {
                if (attacker is ServerPlayerEntity) {
                    attacker.sendMessage(Text.translatable(STAGE1_KEYS.random()), false)
                }
            }
            stage == 2 -> {
                if (!hasWarnedCurrentAggressor) {
                    hasWarnedCurrentAggressor = true
                    if (attacker is ServerPlayerEntity) {
                        attacker.sendMessage(Text.translatable(STAGE2_KEYS.random()), false)
                    }
                }
            }
            stage >= 3 -> {
                if (attacker is ServerPlayerEntity) {
                    attacker.sendMessage(Text.translatable(STAGE3_KEYS.random()), false)
                }
                executeRetaliation(attacker)
            }
        }
    }

    private fun executeRetaliation(attacker: LivingEntity) {
        if (world.isClient) return

        world.sendEntityStatus(this, 4.toByte())
        spawnRetaliateParticles()

        applyParadoxDamage(attacker)
        applyDebuffCombo(attacker)

        when (random.nextInt(3)) {
            0 -> retaliateTeleportVortex(attacker)
            1 -> retaliateHighAltitude(attacker)
            2 -> retaliateParadoxPull(attacker)
        }
    }

    private fun retaliateParadoxPull(attacker: LivingEntity) {
        if (attacker !is ServerPlayerEntity) return
        val targetPos = pos.add(rotationVector.multiply(2.0))
        attacker.teleport(
            attacker.serverWorld,
            targetPos.x, targetPos.y, targetPos.z,
            attacker.yaw, attacker.pitch
        )
        attacker.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3))
        attacker.addStatusEffect(StatusEffectInstance(StatusEffects.NAUSEA, 120, 0))
        attacker.sendMessage(
            Text.translatable("entity.doctor_m.103w_evereye.retaliation.paradox_pull"),
            true
        )
    }

    private fun applyParadoxDamage(attacker: LivingEntity) {
        val paradoxDamage = attacker.maxHealth * 0.2f + 8.0f
        attacker.damage(damageSources.magic(), paradoxDamage)

        if (attacker is ServerPlayerEntity) {
            attacker.sendMessage(
                Text.translatable("entity.doctor_m.103w_evereye.retaliation.paradox_damage"),
                true
            )
        }
    }

    private fun applyDebuffCombo(attacker: LivingEntity) {
        if (attacker is ServerPlayerEntity) {
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.WITHER, 120, 1))
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 300, 2))
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 300, 1))
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0))
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.NAUSEA, 200, 0))
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.MINING_FATIGUE, 400, 2))
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.HUNGER, 200, 1))
            attacker.sendMessage(Text.translatable("entity.doctor_m.103w_evereye.retaliation.debuff"), true)
        } else {
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.WITHER, 100, 1))
            attacker.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1))
        }
    }

    private fun retaliateHighAltitude(attacker: LivingEntity) {
        if (attacker !is ServerPlayerEntity) return
        val targetY = attacker.y + 80 + random.nextInt(50)
        attacker.teleport(attacker.serverWorld, attacker.x, targetY, attacker.z, attacker.yaw, attacker.pitch)
        attacker.addStatusEffect(StatusEffectInstance(StatusEffects.NAUSEA, 200, 0))
        attacker.sendMessage(Text.translatable("entity.doctor_m.103w_evereye.retaliation.high_altitude"), true)
    }

    private fun retaliateTeleportVortex(attacker: LivingEntity) {
        if (attacker !is ServerPlayerEntity) return
        val vortexDim = RegistryKey.of(RegistryKeys.WORLD, Identifier("ait", "time_vortex"))
        val vortexWorld = attacker.server.getWorld(vortexDim)

        if (vortexWorld != null) {
            attacker.teleport(vortexWorld, attacker.x, 350.0, attacker.z, attacker.yaw, attacker.pitch)
        }

        attacker.addStatusEffect(StatusEffectInstance(StatusEffects.WITHER, 160, 2))
        attacker.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 300, 2))
        attacker.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 300, 1))
        attacker.addStatusEffect(StatusEffectInstance(StatusEffects.BLINDNESS, 120, 0))
        attacker.sendMessage(Text.translatable("entity.doctor_m.103w_evereye.retaliation.vortex"), true)
    }

    override fun interactMob(player: PlayerEntity, hand: Hand): ActionResult {
        if (!world.isClient) {
            val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.SUCCESS

            if (isAngry) {
                player.sendMessage(Text.translatable(ANGRY_KEYS.random()), false)
            } else {
                if (player.isSneaking) {
                    tryTrade(serverPlayer)
                } else {
                    player.sendMessage(Text.translatable(PEACEFUL_KEYS.random()), false)
                    player.sendMessage(Text.translatable("entity.doctor_m.103w_evereye.trade.hint.casual"), false)
                    sendTradeList(serverPlayer)
                    player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.hint.sneak"), false)
                    setState(AIState.TRADING)
                }
            }
        }
        return ActionResult.SUCCESS
    }

    private fun sendTradeList(player: ServerPlayerEntity) {
        if (dailyTrades.isEmpty()) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.empty", "玛丽安"), false)
            return
        }
        player.sendMessage(Text.literal("§7════════════════════════"), false)
        for (i in dailyTrades.indices) {
            val offer = dailyTrades[i]
            val status = if (offer.isAvailable) "§e" else "§7§m"
            player.sendMessage(Text.literal("$status[${i + 1}] ${offer.displayText}"), false)
        }
        player.sendMessage(Text.literal("§7════════════════════════"), false)
    }

    private fun tryTrade(player: ServerPlayerEntity) {
        val held = player.mainHandStack
        if (held.isEmpty) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.no_item"), false)
            return
        }

        val matches = dailyTrades.filter {
            it.isAvailable && it.inputItem == held.item
        }.sortedByDescending { it.inputCount }

        if (matches.isEmpty()) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.reject"), false)
            return
        }

        val heldCount = held.count
        for (offer in matches) {
            if (heldCount >= offer.inputCount) {
                offer.execute(player)
                player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.success", "玛丽安"), false)
                grantTradeAdvancement(player)
                return
            }
        }

        player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.insufficient"), false)
    }

    private fun grantTradeAdvancement(player: ServerPlayerEntity) {
        val server = player.server ?: return
        val advancement = server.advancementLoader.get(Identifier("doctor_m", "trading/cross_time_trade"))
        if (advancement != null) {
            player.advancementTracker.grantCriterion(advancement, "impossible")
        }
    }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking(CURRENT_STATE, AIState.IDLE.ordinal)
    }

    fun setState(state: AIState) {
        if (!world.isClient) {
            dataTracker.set(CURRENT_STATE, state.ordinal)
        }
    }

    fun getState(): AIState = AIState.entries[dataTracker.get(CURRENT_STATE)]

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.putBoolean("IsAngry", isAngry)
        nbt.putInt("AngerTimer", angerTimer)
        nbt.putLong("LastRetaliateTime", lastRetaliateTime)
        nbt.putLong("LastAggressionTime", lastAggressionTime)
        nbt.putBoolean("HasWarned", hasWarnedCurrentAggressor)
        nbt.putInt("AggressionCount", aggressionCount)
        lastAggressorUUID?.let { nbt.putUuid("LastAggressor", it) }

        nbt.putLong("LastTradeRefreshDay", lastTradeRefreshDay)
        if (dailyTrades.isNotEmpty()) {
            nbt.put("DailyTrades", TradeManager.writeOffersToNbt(dailyTrades))
        }
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        isAngry = nbt.getBoolean("IsAngry")
        angerTimer = nbt.getInt("AngerTimer")
        lastRetaliateTime = nbt.getLong("LastRetaliateTime")
        lastAggressionTime = nbt.getLong("LastAggressionTime")
        hasWarnedCurrentAggressor = nbt.getBoolean("HasWarned")
        aggressionCount = if (nbt.contains("AggressionCount")) nbt.getInt("AggressionCount") else 0
        if (nbt.contains("LastAggressor")) {
            lastAggressorUUID = nbt.getUuid("LastAggressor")
        }

        lastTradeRefreshDay = if (nbt.contains("LastTradeRefreshDay")) nbt.getLong("LastTradeRefreshDay") else -1
        if (nbt.contains("DailyTrades", 9)) {
            dailyTrades = TradeManager.readOffersFromNbt(nbt.getList("DailyTrades", 10))
        }
    }

    private fun spawnRetaliateParticles() {
        if (world !is ServerWorld) return
        val sw = world as ServerWorld
        sw.spawnParticles(
            ParticleTypes.REVERSE_PORTAL,
            x, y + 1.5, z,
            30, 0.5, 0.5, 0.5, 0.2
        )
        sw.playSound(
            null, blockPos,
            SoundEvents.ENTITY_WITHER_AMBIENT,
            SoundCategory.HOSTILE, 1.0f, 0.8f
        )
    }
}