package doctor_m.entities.data;

import doctor_m.DOCTORM;
import doctor_m.trading.TradeManager;
import doctor_m.trading.TradeOffer;
import mosslib.api.BlinkingEntity;
import mosslib.api.MossAnimatedEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Marian_Jin extends PathAwareEntity implements MossAnimatedEntity , BlinkingEntity {

    public enum AIState { IDLE, TRADING, COMBAT, RETALIATING }

    public static final TrackedData<Integer> CURRENT_STATE =
            DataTracker.registerData(Marian_Jin.class, TrackedDataHandlerRegistry.INTEGER);

    private static final String BASE = "entity.doctor_m.marian_jin";
    public static final String[] STAGE1_KEYS = new String[] {
            BASE + ".dialog.stage1.0", BASE + ".dialog.stage1.1", BASE + ".dialog.stage1.2",
            BASE + ".dialog.stage1.3", BASE + ".dialog.stage1.4"
    };
    public static final String[] STAGE2_KEYS = new String[] {
            BASE + ".dialog.stage2.0", BASE + ".dialog.stage2.1", BASE + ".dialog.stage2.2",
            BASE + ".dialog.stage2.3", BASE + ".dialog.stage2.4"
    };
    public static final String[] STAGE3_KEYS = new String[] {
            BASE + ".dialog.stage3.0", BASE + ".dialog.stage3.1", BASE + ".dialog.stage3.2",
            BASE + ".dialog.stage3.3", BASE + ".dialog.stage3.4"
    };
    public static final String[] PEACEFUL_KEYS = new String[] {
            BASE + ".dialog.peaceful.0", BASE + ".dialog.peaceful.1", BASE + ".dialog.peaceful.2",
            BASE + ".dialog.peaceful.3", BASE + ".dialog.peaceful.4"
    };
    public static final String[] ANGRY_KEYS = new String[] {
            BASE + ".dialog.angry.0", BASE + ".dialog.angry.1", BASE + ".dialog.angry.2",
            BASE + ".dialog.angry.3", BASE + ".dialog.angry.4"
    };
    public static final String[] HURT_KEYS = new String[] {
            BASE + ".dialog.hurt.0", BASE + ".dialog.hurt.1", BASE + ".dialog.hurt.2",
            BASE + ".dialog.hurt.3", BASE + ".dialog.hurt.4"
    };

    // ==================== 眨眼 ====================
    private static final int BLINK_TICKS = 4;   // 0.2 秒
    private int blinkCooldown = 100;             // 首次进游戏 5 秒后眨
    private int blinkDuration = 0;

    private static final int RETALIATE_COOLDOWN = 20;
    private static final long AGGRESSION_MEMORY = 48000L;   // 2 game days
    private static final int ANGER_DURATION = 7200;         // 6 minutes
    private static final String TRADE_POOL_FILE = "marian_trade.json";

    private static final Text TRADE_HEADER = Text.literal("§7════════════════════════");
    private static final String NAME_MARIAN = "玛丽安";

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
                .add(EntityAttributes.GENERIC_ARMOR, 0.0);
    }

    // ==================== Retaliation & memory system ====================
    private long lastRetaliateTime = 0L;
    private boolean isAngry = false;
    private int angerTimer = 0;
    private UUID lastAggressorUUID = null;
    private long lastAggressionTime = 0L;
    private long lastDamageTime = 0L;
    private int aggressionCount = 0;
    private boolean hasWarnedCurrentAggressor = false;

    // ==================== Trading system ====================
    private List<TradeOffer> dailyTrades = new ArrayList<>();
    private long lastTradeRefreshDay = -1L;

    public Marian_Jin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        if (!world.isClient()) {
            lastDamageTime = world.getTime();
        }
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FleeEntityGoal<>(this, HostileEntity.class, 10.0f, 0.4, 0.8));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
        this.goalSelector.add(4, new WanderAroundGoal(this, 0.25));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.4));
        this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
    }

    @Override
    public void tick() {
        super.tick();

        // 客户端：眨眼是纯视觉，不需要同步
        if (this.getWorld().isClient()) {
            tickBlink();
            return;
        }

        // 下面全是服务端逻辑
        if (!(this.getWorld() instanceof ServerWorld sw)) {
            return;
        }

        if (this.isAngry && --this.angerTimer <= 0) {
            calmDown();
        }

        long currentDay = sw.getTime() / 24000L;
        if (currentDay > this.lastTradeRefreshDay) {
            refreshTrades(sw.getServer());
        }

        if (this.getHealth() < this.getMaxHealth()) {
            long now = sw.getTime();
            if (now - this.lastDamageTime > 100 && this.age % 40 == 0) {
                this.heal(1.0f);
            }
        }
    }

    private void calmDown() {
        this.isAngry = false;
        this.hasWarnedCurrentAggressor = false;
        this.aggressionCount = 0;
        this.setTarget(null);
        setState(AIState.IDLE);
    }

    private void refreshTrades(MinecraftServer server) {
        List<TradeOffer> pool = TradeManager.loadPoolFromDatapack(server, TRADE_POOL_FILE);
        this.dailyTrades = TradeManager.generateDailyTrades(pool, this.random);
        this.lastTradeRefreshDay = server.getOverworld().getTime() / 24000L;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (!damaged || this.getWorld().isClient()) {
            return damaged;
        }

        long now = this.getWorld().getTime();
        this.lastDamageTime = now;

        if (this.getHealth() <= 0.0f) {
            return damaged;
        }

        if (!(source.getAttacker() instanceof LivingEntity attacker)) {
            return damaged;
        }

        UUID attackerId = attacker.getUuid();
        boolean isNewAggression = this.lastAggressorUUID == null
                || !this.lastAggressorUUID.equals(attackerId)
                || (now - this.lastAggressionTime) > AGGRESSION_MEMORY;

        if (isNewAggression) {
            this.lastAggressorUUID = attackerId;
            this.lastAggressionTime = now;
            this.hasWarnedCurrentAggressor = false;
            this.aggressionCount = 1;

            if (now - this.lastRetaliateTime >= RETALIATE_COOLDOWN) {
                this.lastRetaliateTime = now;
                handleAggressionStage(attacker, 1);
            }

            if (this.random.nextFloat() < 0.3f && attacker instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable(HURT_KEYS[this.random.nextInt(HURT_KEYS.length)]), true);
            }
            return damaged;
        }

        if (now - this.lastRetaliateTime < RETALIATE_COOLDOWN) {
            return damaged;
        }
        this.lastRetaliateTime = now;
        this.lastAggressionTime = now;
        this.aggressionCount++;

        handleAggressionStage(attacker, this.aggressionCount);
        return damaged;
    }

    private void handleAggressionStage(LivingEntity attacker, int stage) {
        if (!this.isAngry) {
            this.isAngry = true;
            this.angerTimer = ANGER_DURATION;
            this.setTarget(attacker);
            setState(AIState.COMBAT);
        } else {
            this.angerTimer = ANGER_DURATION;
        }

        switch (stage) {
            case 1 -> {
                if (attacker instanceof ServerPlayerEntity player) {
                    player.sendMessage(Text.translatable(STAGE1_KEYS[this.random.nextInt(STAGE1_KEYS.length)]), false);
                }
            }
            case 2 -> {
                if (!this.hasWarnedCurrentAggressor) {
                    this.hasWarnedCurrentAggressor = true;
                    if (attacker instanceof ServerPlayerEntity player) {
                        player.sendMessage(Text.translatable(STAGE2_KEYS[this.random.nextInt(STAGE2_KEYS.length)]), false);
                    }
                }
            }
            default -> {
                if (attacker instanceof ServerPlayerEntity player) {
                    player.sendMessage(Text.translatable(STAGE3_KEYS[this.random.nextInt(STAGE3_KEYS.length)]), false);
                }
                executeRetaliation(attacker);
            }
        }
    }

    private void executeRetaliation(LivingEntity attacker) {
        if (this.getWorld().isClient()) return;

        this.getWorld().sendEntityStatus(this, (byte) 4);
        spawnRetaliateParticles();

        applyParadoxDamage(attacker);
        applyDebuffCombo(attacker);

        switch (this.random.nextInt(3)) {
            case 0 -> retaliateTeleportVortex(attacker);
            case 1 -> retaliateHighAltitude(attacker);
            case 2 -> retaliateParadoxPull(attacker);
        }
    }

    private void retaliateParadoxPull(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        double targetX = this.getX() + this.getRotationVector().x * 2.0;
        double targetY = this.getY() + this.getRotationVector().y * 2.0;
        double targetZ = this.getZ() + this.getRotationVector().z * 2.0;
        player.teleport(player.getServerWorld(), targetX, targetY, targetZ, player.getYaw(), player.getPitch());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
        player.sendMessage(Text.translatable("entity.doctor_m.marian_jin.retaliation.paradox_pull"), true);
    }

    private void applyParadoxDamage(LivingEntity attacker) {
        float paradoxDamage = attacker.getMaxHealth() * 0.2f + 8.0f;
        attacker.damage(this.getDamageSources().magic(), paradoxDamage);

        if (attacker instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("entity.doctor_m.marian_jin.retaliation.paradox_damage"), true);
        }
    }

    private void applyDebuffCombo(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 400, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, 1));
            player.sendMessage(Text.translatable("entity.doctor_m.marian_jin.retaliation.debuff"), true);
        } else {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1));
        }
    }

    private void retaliateHighAltitude(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        double targetY = player.getY() + 80 + this.random.nextInt(50);
        player.teleport(player.getServerWorld(), player.getX(), targetY, player.getZ(), player.getYaw(), player.getPitch());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
        player.sendMessage(Text.translatable("entity.doctor_m.marian_jin.retaliation.high_altitude"), true);
    }

    private void retaliateTeleportVortex(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        RegistryKey<World> vortexDim = RegistryKey.of(RegistryKeys.WORLD, new Identifier("ait", "time_vortex"));
        ServerWorld vortexWorld = player.getServer().getWorld(vortexDim);

        if (vortexWorld != null) {
            player.teleport(vortexWorld, player.getX(), 350.0, player.getZ(), player.getYaw(), player.getPitch());
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 160, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 0));
            player.sendMessage(Text.translatable("entity.doctor_m.marian_jin.retaliation.vortex"), true);
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient()) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.SUCCESS;

        if (this.isAngry) {
            player.sendMessage(Text.translatable(ANGRY_KEYS[this.random.nextInt(ANGRY_KEYS.length)]), false);
        } else {
            if (player.isSneaking()) {
                tryTrade(serverPlayer);
            } else {
                player.sendMessage(Text.translatable(PEACEFUL_KEYS[this.random.nextInt(PEACEFUL_KEYS.length)]), false);
                player.sendMessage(Text.translatable("entity.doctor_m.marian_jin.trade.hint.casual"), false);
                sendTradeList(serverPlayer);
                player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.hint.sneak"), false);
                setState(AIState.TRADING);
            }
        }
        return ActionResult.SUCCESS;
    }

    private void sendTradeList(ServerPlayerEntity player) {
        if (this.dailyTrades.isEmpty()) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.empty", NAME_MARIAN), false);
            return;
        }
        player.sendMessage(TRADE_HEADER, false);
        for (int i = 0; i < this.dailyTrades.size(); i++) {
            TradeOffer offer = this.dailyTrades.get(i);
            String status = offer.isAvailable() ? "§e" : "§7§m";
            player.sendMessage(Text.literal(status + "[" + (i + 1) + "] " + offer.getDisplayText()), false);
        }
        player.sendMessage(TRADE_HEADER, false);
    }

    private void tryTrade(ServerPlayerEntity player) {
        var held = player.getMainHandStack();
        if (held.isEmpty()) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.no_item"), false);
            return;
        }

        var heldItem = held.getItem();
        int heldCount = held.getCount();
        TradeOffer bestMatch = null;
        boolean hasMatchingItem = false;

        for (TradeOffer offer : this.dailyTrades) {
            if (!offer.isAvailable() || offer.getInputItem() != heldItem) continue;
            hasMatchingItem = true;
            if (heldCount >= offer.getInputCount()) {
                if (bestMatch == null || offer.getInputCount() > bestMatch.getInputCount()) {
                    bestMatch = offer;
                }
            }
        }

        if (bestMatch != null) {
            bestMatch.execute(player);
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.success", NAME_MARIAN), false);
            grantTradeAdvancement(player);
        } else if (hasMatchingItem) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.insufficient"), false);
        } else {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.reject"), false);
        }
    }

    private void grantTradeAdvancement(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        var advancement = server.getAdvancementLoader().get(new Identifier("doctor_m", "marian_trade"));
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "impossible");
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CURRENT_STATE, AIState.IDLE.ordinal());
    }

    public void setState(AIState state) {
        if (!this.getWorld().isClient()) {
            this.dataTracker.set(CURRENT_STATE, state.ordinal());
        }
    }

    public AIState getState() {
        return AIState.values()[this.dataTracker.get(CURRENT_STATE)];
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("IsAngry", this.isAngry);
        nbt.putInt("AngerTimer", this.angerTimer);
        nbt.putLong("LastRetaliateTime", this.lastRetaliateTime);
        nbt.putLong("LastAggressionTime", this.lastAggressionTime);
        nbt.putBoolean("HasWarned", this.hasWarnedCurrentAggressor);
        nbt.putInt("AggressionCount", this.aggressionCount);
        if (this.lastAggressorUUID != null) {
            nbt.putUuid("LastAggressor", this.lastAggressorUUID);
        }

        nbt.putLong("LastTradeRefreshDay", this.lastTradeRefreshDay);
        if (!this.dailyTrades.isEmpty()) {
            nbt.put("DailyTrades", TradeManager.writeOffersToNbt(this.dailyTrades));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.isAngry = nbt.getBoolean("IsAngry");
        this.angerTimer = nbt.getInt("AngerTimer");
        this.lastRetaliateTime = nbt.getLong("LastRetaliateTime");
        this.lastAggressionTime = nbt.getLong("LastAggressionTime");
        this.hasWarnedCurrentAggressor = nbt.getBoolean("HasWarned");
        this.aggressionCount = nbt.contains("AggressionCount") ? nbt.getInt("AggressionCount") : 0;
        if (nbt.contains("LastAggressor")) {
            this.lastAggressorUUID = nbt.getUuid("LastAggressor");
        }

        this.lastTradeRefreshDay = nbt.contains("LastTradeRefreshDay") ? nbt.getLong("LastTradeRefreshDay") : -1;
        if (nbt.contains("DailyTrades", 9)) {
            this.dailyTrades = TradeManager.readOffersFromNbt(nbt.getList("DailyTrades", 10));
        }
    }

    private void spawnRetaliateParticles() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        sw.spawnParticles(
                ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY() + 1.5, this.getZ(),
                30, 0.5, 0.5, 0.5, 0.2
        );
        sw.playSound(
                null, this.getBlockPos(),
                SoundEvents.ENTITY_WITHER_AMBIENT,
                SoundCategory.HOSTILE, 1.0f, 0.8f
        );
    }

    // =====================================================================
    // MossBedrock 集成
    // =====================================================================

    /** 上一次解析出的动画 ID，用于检测切换。 */
    @Nullable
    private Identifier mossLastAnimId = null;

    /** 上一次动画切换时的 age。用于让动画切换时归零。 */
    private int mossAnimStartAge = 0;

    // ── MossBedrockRenderable ──

    @Override
    public Identifier getMossModel() {
        return Identifier.of(DOCTORM.MOD_ID, "marian_jin");
    }

    @Override
    public Identifier getMossTexture() {
        // 你的纹理路径 —— 按实际改
        return Identifier.of(DOCTORM.MOD_ID, "textures/entity/marian_jin.png");
    }

    @Override
    public Identifier getMossEmission() {
        return null;   // 没有发光层
    }

    @Override
    public float getMossYaw(float tickDelta) {
        return 0f;     // 实体渲染走 bodyYaw，这个不用
    }

    // ── MossAnimatedEntity ──

    @Override
    public net.minecraft.entity.Entity asEntity() {
        return this;
    }

    @Override
    public Identifier getMossAnimationId() {
        Identifier next = computeMossAnimId();

        // 检测切换 —— 换动画时把起始年龄归零
        if (!java.util.Objects.equals(next, mossLastAnimId)) {
            mossLastAnimId = next;
            mossAnimStartAge = this.age;
        }

        return next;
    }

    @Override
    public long getAnimationElapsedMs() {
        return (long) (this.age - mossAnimStartAge) * 50L;
    }

    /**
     * 根据实体当前状态决定用哪个动画。
     * 返回 null 表示"这一帧不播 Bedrock 动画" → 渲染器走原版回退。
     */
    @Nullable
    private Identifier computeMossAnimId() {
        // 优先级从高到低

        // 1. 生气状态 → 愤怒动画
        if (this.isAngry) {
            return Identifier.of(DOCTORM.MOD_ID, "marian_jin/angry");
        }

        // 2. 交易状态 → 交易动画（如果 K 了的话）
        //    没 K 的话这行删掉，让它走 idle
        if (getState() == AIState.TRADING) {
            return Identifier.of(DOCTORM.MOD_ID, "marian_jin/trade");
        }

        // 3. 移动中 → 走路动画
        if (this.getVelocity().horizontalLengthSquared() > 1.0E-4) {
            return Identifier.of(DOCTORM.MOD_ID, "marian_jin/walk");
        }

        // 4. 默认 → 站立
        return Identifier.of(DOCTORM.MOD_ID, "marian_jin/idle");
    }

    private void tickBlink() {
        if (blinkDuration > 0) {
            blinkDuration--;
            if (blinkDuration == 0) {
                // 眨完一次，安排下一次
                blinkCooldown = 60 + this.random.nextInt(180);   // 3~12 秒
            }
        } else {
            blinkCooldown--;
            if (blinkCooldown <= 0) {
                blinkDuration = BLINK_TICKS;
            }
        }
    }

    @Override
    public boolean isBlinking() {
        return blinkDuration > 0;
    }

    @Override
    public float getBlinkScale() {
        if (blinkDuration <= 0) return 1.0f;
        int elapsed = BLINK_TICKS - blinkDuration;
        float t = (float) elapsed / BLINK_TICKS;
        // 三角波：0 → 1 → 0
        float closed = t < 0.5f ? t * 2f : (1f - t) * 2f;
        return 1.0f - closed * 0.9f;   // 1.0 → 0.1
    }
}