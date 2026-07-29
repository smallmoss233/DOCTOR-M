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

        fun createMobAttributes(): DefaultAttributeContainer.Builder =
            PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
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

    // 新增：对话链阶段控制
    private var aggressionCount = 0
    private var hasWarnedCurrentAggressor = false

    private val RETALIATE_COOLDOWN = 20
    private val AGGRESSION_MEMORY = 600L
    private val ANGER_DURATION = 400

    // ==================== 交易系统（新增）====================
    private var dailyTrades: MutableList<TradeOffer> = ArrayList()
    private var lastTradeRefreshDay = -1L
    private val TRADE_POOL_FILE = "trades_evereye.json"

    // ==================== 对话池 ====================
    // 阶段1：首次受击 - 任性/不讲理/孩子气
    private val stage1Reactions = listOf(
        "§4§l[玛丽安] §r§c你干嘛打我！很痛的诶！",
        "§4§l[玛丽安] §r§c呜...你欺负人！我要还手了！",
        "§4§l[玛丽安] §r§c住手！你知道我是谁吗就敢碰我？",
        "§4§l[玛丽安] §r§c好过分...时间领主都没打过我！",
        "§4§l[玛丽安] §r§c你完了！我要在你的时间线上画涂鸦！"
    )

    // 阶段2：再次受击 - 霸道威胁/小孩子放狠话
    private val stage2Warnings = listOf(
        "§4§l[玛丽安] §r§c我真的生气了！后果很严重！",
        "§4§l[玛丽安] §r§c我要告诉我哥哥！让他把你从历史里删掉！",
        "§4§l[玛丽安] §r§c最后一次警告！不然我把你变成青蛙！",
        "§4§l[玛丽安] §r§c你知不知道我发脾气很可怕的？",
        "§4§l[玛丽安] §r§c哼！你以为我不敢打你吗？"
    )

    // 阶段3：反击时 - 霸道+孩子气的混合
    private val stage3Retaliations = listOf(
        "§4§l[玛丽安] §r§c让你打！让你打！现在知道错了吧！",
        "§4§l[玛丽安] §r§c呜啊啊啊——去死吧去死吧！",
        "§4§l[玛丽安] §r§c这是你逼我的！我才不想这样呢！",
        "§4§l[玛丽安] §r§c哼！被打了吧？活该！",
        "§4§l[玛丽安] §r§c我要把你关进时间角落，永远不许出来！"
    )

    // 交互对话 - 平静时（霸道小商贩）
    private val peacefulInteractions = listOf(
        "§5§l[玛丽安] §r§7呵，想要完整的子系统？还是引擎？下界之星我也有不少...",
        "§5§l[玛丽安] §r§7我的东西可是很贵的，买不起就别碰我。",
        "§5§l[玛丽安] §r§7看什么看？要买就买，不买就走开。",
        "§5§l[玛丽安] §r§7今天心情好，给你打个...呃，九九折吧。",
        "§5§l[玛丽安] §r§7这些都是我从各个时间线淘来的宝贝，便宜你了。"
    )

    // 交互对话 - 生气时（傲娇拒绝）
    private val angryInteractions = listOf(
        "§4§l[玛丽安] §r§c我现在很生气，不想跟你说话！",
        "§4§l[玛丽安] §r§c哼！刚才打我现在还想买东西？做梦！",
        "§4§l[玛丽安] §r§c走开走开！看到你我就烦！",
        "§4§l[玛丽安] §r§c除非你给我道歉...不然免谈！",
        "§4§l[玛丽安] §r§c我的店今天对你关门！永远！"
    )

    // 受击时额外喊话（30%概率）
    private val hurtReactions = listOf(
        "§c§o好痛...你这个坏蛋！",
        "§c§o呜...我会记住你的！",
        "§c§o你敢打我？你完了！",
        "§c§o妈妈...啊不是，时间领主——有人欺负我！",
        "§c§o我要在你的床上放时间蠕虫！"
    )

    override fun initGoals() {
        super.initGoals()
        goalSelector.add(0, SwimGoal(this))
        goalSelector.add(2, LookAtEntityGoal(this, PlayerEntity::class.java, 12.0f))
        goalSelector.add(3, LookAroundGoal(this))
        goalSelector.add(4, WanderAroundGoal(this, 0.6))
        goalSelector.add(5, WanderAroundFarGoal(this, 0.5))
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

        // 新增：每日交易刷新
        if (!world.isClient && world is ServerWorld) {
            val sw = world as ServerWorld
            val currentDay = sw.time / 24000L
            if (currentDay > lastTradeRefreshDay || dailyTrades.isEmpty()) {
                refreshTrades(sw.server)
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

    // ==================== 交易刷新（新增）====================
    private fun refreshTrades(server: MinecraftServer) {
        val pool = TradeManager.loadPoolFromDatapack(server, TRADE_POOL_FILE)
        dailyTrades = TradeManager.generateDailyTrades(pool, random)
        lastTradeRefreshDay = server.overworld.time / 24000L
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        val damaged = super.damage(source, amount)
        if (!damaged || world.isClient) return damaged
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

            // 30%概率触发孩子气受击喊话
            if (random.nextFloat() < 0.3f && attacker is ServerPlayerEntity) {
                attacker.sendMessage(Text.literal(hurtReactions.random()), true)
            }
            return damaged
        }

        // 同一攻击者持续攻击
        if (now - lastRetaliateTime < RETALIATE_COOLDOWN) return damaged
        lastRetaliateTime = now
        lastAggressionTime = now
        aggressionCount++

        handleAggressionStage(attacker, aggressionCount)
        return damaged
    }

    /**
     * 处理攻击阶段：霸道不讲理+小孩子气的对话链
     * 阶段1：任性抱怨（请求停火）
     * 阶段2：放狠话警告（最后警告）
     * 阶段3+：直接反击
     */
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
                // 首次受击：不讲理地抱怨
                if (attacker is ServerPlayerEntity) {
                    attacker.sendMessage(Text.literal(stage1Reactions.random()), false)
                }
            }
            stage == 2 -> {
                // 第二次受击：霸道警告（孩子气放狠话）
                if (!hasWarnedCurrentAggressor) {
                    hasWarnedCurrentAggressor = true
                    if (attacker is ServerPlayerEntity) {
                        attacker.sendMessage(Text.literal(stage2Warnings.random()), false)
                    }
                }
            }
            stage >= 3 -> {
                // 第三次及以上：直接反击，同时喊话
                if (attacker is ServerPlayerEntity) {
                    attacker.sendMessage(Text.literal(stage3Retaliations.random()), false)
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

    // 新增：悖论牵引（霸道不讲理地拉近距离）
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
            Text.literal("§4§o§l休想逃！给我过来！——玛丽安蛮横地将你拽到了面前。"),
            true
        )
    }

    private fun applyParadoxDamage(attacker: LivingEntity) {
        val paradoxDamage = attacker.maxHealth * 0.2f + 8.0f
        attacker.damage(damageSources.magic(), paradoxDamage)

        if (attacker is ServerPlayerEntity) {
            attacker.sendMessage(
                Text.literal("§4§l你遭到了悖论打击！§r§7现实在你周围崩塌..."),
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
            attacker.sendMessage(Text.literal("§5§o时间悖论侵蚀了你的存在..."), true)
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
        attacker.sendMessage(Text.literal("§4§o被抛向高空，感受失重吧。"), true)
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
        attacker.sendMessage(Text.literal("§4§l§o玛丽安：时间涡旋会教你什么是尊重他人。"), true)
    }

    // ==================== 交互系统（已集成交易）====================
    override fun interactMob(player: PlayerEntity, hand: Hand): ActionResult {
        if (!world.isClient) {
            val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.SUCCESS

            if (isAngry) {
                player.sendMessage(Text.literal(angryInteractions.random()), false)
            } else {
                if (player.isSneaking) {
                    // 蹲下右键 = 确认交易，不再刷屏
                    tryTrade(serverPlayer)
                } else {
                    // 普通右键 = 只看列表+打招呼
                    player.sendMessage(Text.literal(peacefulInteractions.random()), false)
                    player.sendMessage(Text.literal("§8§o（她看上去很随意...）"), false)
                    sendTradeList(serverPlayer)
                    player.sendMessage(Text.literal("§7§o手持对应数量的物品 §e§l蹲下右键§r§7 确认交易"), false)
                    setState(AIState.TRADING)
                }
            }
        }
        return ActionResult.SUCCESS
    }

    // ==================== 交易辅助方法（新增）====================
    private fun sendTradeList(player: ServerPlayerEntity) {
        if (dailyTrades.isEmpty()) {
            player.sendMessage(Text.literal("§7§o（玛丽安 今天没有东西可卖。）"), false)
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

    /**
     * 智能交易匹配：
     * 同输入物品时，优先买 inputCount 最大的（贵的），买不起再降级
     */
    private fun tryTrade(player: ServerPlayerEntity) {
        val held = player.mainHandStack
        if (held.isEmpty) {
            player.sendMessage(Text.literal("§7§o你手里空空如也，拿什么买？"), false)
            return
        }

        val matches = dailyTrades.filter {
            it.isAvailable && it.inputItem == held.item
        }.sortedByDescending { it.inputCount }

        if (matches.isEmpty()) {
            player.sendMessage(Text.literal("§c§o你手里的东西我不收。"), false)
            return
        }

        val heldCount = held.count
        for (offer in matches) {
            if (heldCount >= offer.inputCount) {
                offer.execute(player)
                player.sendMessage(Text.literal("§a§l[玛丽安] §r§a成交！这是你的货。"), false)
                grantTradeAdvancement(player)
                return
            }
        }

        player.sendMessage(Text.literal("§c§o你手里的数量不够..."), false)
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

    // ==================== NBT 序列化（已包含交易数据）====================
    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        nbt.putBoolean("IsAngry", isAngry)
        nbt.putInt("AngerTimer", angerTimer)
        nbt.putLong("LastRetaliateTime", lastRetaliateTime)
        nbt.putLong("LastAggressionTime", lastAggressionTime)
        nbt.putBoolean("HasWarned", hasWarnedCurrentAggressor)
        nbt.putInt("AggressionCount", aggressionCount)
        lastAggressorUUID?.let { nbt.putUuid("LastAggressor", it) }

        // 交易数据持久化
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

        // 交易数据读取
        lastTradeRefreshDay = if (nbt.contains("LastTradeRefreshDay")) nbt.getLong("LastTradeRefreshDay") else -1
        if (nbt.contains("DailyTrades", 9)) { // 9 = NbtElement.LIST_TYPE
            dailyTrades = TradeManager.readOffersFromNbt(nbt.getList("DailyTrades", 10)) // 10 = NbtElement.COMPOUND_TYPE
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