package doctor_m.entities.data;

import dev.amble.ait.core.AITSounds;
import dev.amble.ait.module.gun.core.item.GunItems;
import dev.amble.ait.module.gun.core.item.StaserBoltMagazine;
import doctor_m.trading.TradeManager;
import doctor_m.trading.TradeOffer;
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
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

public class Entity103Tardis extends PathAwareEntity {

    // ==================== 同步数据 ====================
    private static final TrackedData<String> SELECTED_SKIN =
            DataTracker.registerData(Entity103Tardis.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> MODEL_TYPE =
            DataTracker.registerData(Entity103Tardis.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> CURRENT_STATE =
            DataTracker.registerData(Entity103Tardis.class, TrackedDataHandlerRegistry.INTEGER);

    // ==================== 皮肤与显示 ====================
    private String selectedSkin = "";
    private String displayName = "";
    private String modelType = "slim";

    // ==================== 个性系统 ====================
    public enum Personality {
        AGGRESSIVE, DEFENSIVE, TIMID, BRAVE, TRADER
    }

    private Personality personality = Personality.TRADER;

    // ==================== 反击与记忆系统 ====================
    private UUID lastAggressorUUID = null;
    private long lastAggressionTime = 0;
    private static final long AGGRESSION_MEMORY = 600L;
    private boolean hasWarnedCurrentAggressor = false;
    private int aggressionCount = 0;
    private long lastDamageTime = 0;

    // 反击控制
    private long targetingPlayerUntil = 0;
    private int nonStaserRetaliationCooldown = 0;
    private boolean timidHasRetaliated = false;

    // ==================== 交易系统 ====================
    private List<TradeOffer> dailyTrades = new ArrayList<>();
    private long lastTradeRefreshDay = -1;
    private static final String TRADE_POOL_FILE = "103tardis_trade.json";

    // ==================== AI 状态机 ====================
    public enum AIState {
        IDLE(0), TRADING(1), FLEEING(2), RETALIATING(3), COMBAT(4);
        final int id;
        AIState(int id) { this.id = id; }
    }

    // ==================== 反击类型 ====================
    public enum RetaliateType {
        MELEE, ENERGY_BEAM, HIGH_ALTITUDE, TELEPORT_TRENZALORE, TELEPORT_VORTEX
    }

    // ==================== 远程攻击冷却 ====================
    private int rangedCooldown = 0;

    // ==================== 性能优化：预编译正则 & 缓存配置 ====================
    private static final Pattern SKIN_NAME_PATTERN = Pattern.compile("[^a-z0-9._-]");

    private static final class PersonalityConfig {
        final String lowerName;
        final int hurtCount;
        final int ceasefireCount;
        final int warnCount;
        final int interactCount;
        final RetaliateType[] weightedTypes;
        final int[] weightPrefixSum;

        PersonalityConfig(Personality p, Map<RetaliateType, Integer> weights) {
            this.lowerName = p.name().toLowerCase();
            this.hurtCount = 2;
            this.ceasefireCount = (p == Personality.AGGRESSIVE) ? 0 : 2;
            this.warnCount = (p == Personality.AGGRESSIVE) ? 0 : 2;
            this.interactCount = (p == Personality.TRADER) ? 0 : 3;

            int total = 0;
            int idx = 0;
            this.weightedTypes = new RetaliateType[weights.size()];
            this.weightPrefixSum = new int[weights.size()];
            for (Map.Entry<RetaliateType, Integer> e : weights.entrySet()) {
                total += e.getValue();
                this.weightedTypes[idx] = e.getKey();
                this.weightPrefixSum[idx] = total;
                idx++;
            }
        }
    }

    private static final Map<Personality, PersonalityConfig> PERSONALITY_CONFIGS;
    private static final Map<Personality, Map<RetaliateType, Integer>> RETALIATE_WEIGHTS = new EnumMap<>(Personality.class);

    static {
        Map<RetaliateType, Integer> aggressive = new EnumMap<>(RetaliateType.class);
        aggressive.put(RetaliateType.ENERGY_BEAM, 35);
        aggressive.put(RetaliateType.MELEE, 20);
        aggressive.put(RetaliateType.HIGH_ALTITUDE, 25);
        aggressive.put(RetaliateType.TELEPORT_VORTEX, 15);
        aggressive.put(RetaliateType.TELEPORT_TRENZALORE, 5);
        RETALIATE_WEIGHTS.put(Personality.AGGRESSIVE, aggressive);

        Map<RetaliateType, Integer> defensive = new EnumMap<>(RetaliateType.class);
        defensive.put(RetaliateType.ENERGY_BEAM, 30);
        defensive.put(RetaliateType.TELEPORT_TRENZALORE, 30);
        defensive.put(RetaliateType.MELEE, 20);
        defensive.put(RetaliateType.HIGH_ALTITUDE, 15);
        defensive.put(RetaliateType.TELEPORT_VORTEX, 5);
        RETALIATE_WEIGHTS.put(Personality.DEFENSIVE, defensive);

        Map<RetaliateType, Integer> timid = new EnumMap<>(RetaliateType.class);
        timid.put(RetaliateType.TELEPORT_VORTEX, 35);
        timid.put(RetaliateType.HIGH_ALTITUDE, 30);
        timid.put(RetaliateType.ENERGY_BEAM, 20);
        timid.put(RetaliateType.TELEPORT_TRENZALORE, 10);
        timid.put(RetaliateType.MELEE, 5);
        RETALIATE_WEIGHTS.put(Personality.TIMID, timid);

        Map<RetaliateType, Integer> brave = new EnumMap<>(RetaliateType.class);
        brave.put(RetaliateType.ENERGY_BEAM, 30);
        brave.put(RetaliateType.MELEE, 30);
        brave.put(RetaliateType.HIGH_ALTITUDE, 25);
        brave.put(RetaliateType.TELEPORT_TRENZALORE, 10);
        brave.put(RetaliateType.TELEPORT_VORTEX, 5);
        RETALIATE_WEIGHTS.put(Personality.BRAVE, brave);

        Map<RetaliateType, Integer> trader = new EnumMap<>(RetaliateType.class);
        trader.put(RetaliateType.MELEE, 40);
        trader.put(RetaliateType.ENERGY_BEAM, 30);
        trader.put(RetaliateType.HIGH_ALTITUDE, 20);
        trader.put(RetaliateType.TELEPORT_TRENZALORE, 8);
        trader.put(RetaliateType.TELEPORT_VORTEX, 2);
        RETALIATE_WEIGHTS.put(Personality.TRADER, trader);

        Map<Personality, PersonalityConfig> configs = new EnumMap<>(Personality.class);
        for (Personality p : Personality.values()) {
            configs.put(p, new PersonalityConfig(p, RETALIATE_WEIGHTS.get(p)));
        }
        PERSONALITY_CONFIGS = Collections.unmodifiableMap(configs);
    }

    private PersonalityConfig personalityConfig;

    // ==================== 构造 ====================
    public Entity103Tardis(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        if (!world.isClient) {
            this.goalSelector.clear(goal -> true);
            this.targetSelector.clear(goal -> true);

            Personality[] values = Personality.values();
            this.personality = values[this.random.nextInt(values.length)];
            this.personalityConfig = PERSONALITY_CONFIGS.get(this.personality);
            this.lastDamageTime = world.getTime();
            chooseRandomSkin();
            setState(AIState.IDLE);

            this.initGoals();
        }
    }

    // ==================== AI 初始化 ====================
    @Override
    protected void initGoals() {
        super.initGoals();
        if (this.personality == null) {
            this.personality = Personality.TRADER;
            this.personalityConfig = PERSONALITY_CONFIGS.get(this.personality);
        }
        this.goalSelector.add(0, new SwimGoal(this));
        initCombatGoals();
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));
        initMovementGoals();
    }

    private void initCombatGoals() {
        switch (personality) {
            case AGGRESSIVE -> {
                this.targetSelector.add(1, new RevengeGoal(this));
                this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
                this.targetSelector.add(3, new ActiveTargetGoal<>(this, HostileEntity.class, true));
                this.targetSelector.add(4, new ActiveTargetGoal<>(this, LivingEntity.class, true,
                        entity -> !(entity instanceof Entity103Tardis) && !(entity instanceof PlayerEntity)));
                this.goalSelector.add(2, new TardisAttackGoal(this, 1.2));
            }
            case BRAVE -> {
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, true));
                this.goalSelector.add(2, new TardisAttackGoal(this, 1.1));
            }
            case DEFENSIVE -> {
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, true));
                this.goalSelector.add(2, new TardisAttackGoal(this, 1.1));
            }
            case TIMID -> {
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                this.targetSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 12.0f, 1.0, 1.4));
                this.goalSelector.add(2, new FleeEntityGoal<>(this, HostileEntity.class, 10.0f, 1.0, 1.4));
            }
            case TRADER -> {
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                this.goalSelector.add(2, new FleeEntityGoal<>(this, HostileEntity.class, 10.0f, 0.8, 1.0));
            }
        }
    }

    private void initMovementGoals() {
        switch (personality) {
            case AGGRESSIVE -> {
                this.goalSelector.add(5, new WanderAroundGoal(this, 0.7));
                this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.6));
            }
            case BRAVE -> this.goalSelector.add(5, new WanderAroundGoal(this, 0.8));
            case DEFENSIVE -> this.goalSelector.add(5, new WanderAroundGoal(this, 0.5));
            case TIMID -> this.goalSelector.add(5, new WanderAroundGoal(this, 0.4));
            case TRADER -> {
                this.goalSelector.add(5, new WanderAroundGoal(this, 0.55));
                this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.5));
            }
        }
    }

    // ==================== 自定义攻击目标（近战 + Staser Bolt） ====================
    private static class TardisAttackGoal extends Goal {
        private final Entity103Tardis tardis;
        private final double speed;
        private LivingEntity target;
        private int updateCountdownTicks;
        private int meleeCooldown = 0;
        private int rangedAttackTicks = 0;
        private boolean rangedAttackPreparing = false;

        public TardisAttackGoal(Entity103Tardis tardis, double speed) {
            this.tardis = tardis;
            this.speed = speed;
        }

        @Override
        public boolean canStart() {
            LivingEntity living = this.tardis.getTarget();
            if (living == null || !living.isAlive()) return false;
            this.target = living;
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return this.target != null && this.target.isAlive() && this.tardis.getTarget() == this.target;
        }

        @Override
        public void stop() {
            this.target = null;
            this.updateCountdownTicks = 0;
            this.meleeCooldown = 0;
            this.rangedAttackTicks = 0;
            this.rangedAttackPreparing = false;
            this.tardis.clearActiveItem();
        }

        @Override
        public void tick() {
            if (this.target == null) return;
            this.tardis.getLookControl().lookAt(this.target, 30.0f, 30.0f);
            double distanceSq = this.tardis.squaredDistanceTo(this.target);
            double meleeRangeSq = 3.0 * 3.0;

            this.updateCountdownTicks = Math.max(this.updateCountdownTicks - 1, 0);
            if (this.updateCountdownTicks <= 0) {
                this.tardis.getNavigation().startMovingTo(this.target, this.speed);
                this.updateCountdownTicks = 10;
            }

            if (this.meleeCooldown > 0) this.meleeCooldown--;
            if (this.tardis.rangedCooldown > 0) this.tardis.rangedCooldown--;

            if (this.rangedAttackPreparing) {
                this.rangedAttackTicks--;
                if (this.rangedAttackTicks <= 0) {
                    this.tardis.shootStaserBolt(this.target);
                    this.tardis.shootStaserBolt(this.target);
                    this.rangedAttackPreparing = false;
                    this.tardis.clearActiveItem();
                    this.tardis.rangedCooldown = 10;
                }
                return;
            }

            if (distanceSq <= meleeRangeSq) {
                if (this.meleeCooldown <= 0) {
                    this.tardis.tryAttack(this.target);
                    this.tardis.swingHand(Hand.MAIN_HAND);
                    this.meleeCooldown = 20;
                }
            } else if (this.tardis.rangedCooldown <= 0 && this.tardis.canSee(this.target)) {
                this.rangedAttackPreparing = true;
                this.rangedAttackTicks = 10;
                if (this.tardis.getMainHandStack().isEmpty()) {
                    this.tardis.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.BOW));
                }
                this.tardis.setCurrentHand(Hand.MAIN_HAND);
            }
        }
    }

    // ==================== Tick ====================
    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient || !(this.getWorld() instanceof ServerWorld sw)) return;

        if (this.rangedCooldown > 0) this.rangedCooldown--;
        if (this.nonStaserRetaliationCooldown > 0) this.nonStaserRetaliationCooldown--;

        if (this.targetingPlayerUntil > 0) {
            long now = sw.getTime();
            if (now > this.targetingPlayerUntil) {
                this.targetingPlayerUntil = 0;
                if (this.getTarget() instanceof PlayerEntity) this.setTarget(null);
            } else if (this.getTarget() instanceof PlayerEntity targetPlayer && !this.canSee(targetPlayer)) {
                this.setTarget(null);
                this.targetingPlayerUntil = 0;
            }
        }

        long currentDay = sw.getTime() / 24000L;
        if (currentDay > lastTradeRefreshDay) {
            refreshTrades(sw.getServer());
        }

        if (this.getHealth() < this.getMaxHealth()) {
            long now = sw.getTime();
            if (now - lastDamageTime > 100 && this.age % 40 == 0) {
                this.heal(2.0f);
            }
        }
    }

    private void refreshTrades(MinecraftServer server) {
        List<TradeOffer> pool = TradeManager.loadPoolFromDatapack(server, TRADE_POOL_FILE);
        this.dailyTrades = TradeManager.generateDailyTrades(pool, this.random);
        this.lastTradeRefreshDay = server.getOverworld().getTime() / 24000L;
    }

    // ==================== 受伤与对话链 ====================
    @Override
    public boolean damage(DamageSource source, float amount) {
        amount *= 0.5f;

        boolean damaged = super.damage(source, amount);
        if (!damaged || this.getWorld().isClient) return damaged;

        lastDamageTime = this.getWorld().getTime();

        if (this.isDead() || this.getHealth() <= 0.0f) return damaged;
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return damaged;

        long now = this.getWorld().getTime();
        UUID attackerId = attacker.getUuid();

        boolean isNewAggression = lastAggressorUUID == null
                || !lastAggressorUUID.equals(attackerId)
                || (now - lastAggressionTime) > AGGRESSION_MEMORY;

        if (isNewAggression) {
            lastAggressorUUID = attackerId;
            lastAggressionTime = now;
            aggressionCount = 1;
            hasWarnedCurrentAggressor = false;
        } else {
            lastAggressionTime = now;
            aggressionCount++;
        }

        if (attacker instanceof ServerPlayerEntity player) {
            if (personality == Personality.TIMID) {
                if (!timidHasRetaliated) {
                    timidHasRetaliated = true;
                    shootStaserBolt(player);
                    shootStaserBolt(player);
                }
                sendHurtReaction(player);
                return damaged;
            }

            switch (personality) {
                case AGGRESSIVE -> {
                    if (aggressionCount == 1) {
                        setTarget(player);
                        targetingPlayerUntil = now + 1200;
                    } else if (targetingPlayerUntil > 0 && nonStaserRetaliationCooldown <= 0) {
                        nonStaserRetaliationCooldown = 100;
                        executeNonStaserRetaliation(player);
                    }
                }
                case BRAVE, DEFENSIVE, TRADER -> {
                    if (aggressionCount == 1) {
                        requestCeaseFire(player);
                    } else if (aggressionCount == 2) {
                        warnBeforeRetaliation(player);
                    } else if (aggressionCount == 3) {
                        setTarget(player);
                        targetingPlayerUntil = now + 1200;
                    } else if (targetingPlayerUntil > 0 && nonStaserRetaliationCooldown <= 0) {
                        nonStaserRetaliationCooldown = 100;
                        executeNonStaserRetaliation(player);
                    }
                }
            }

            if (this.random.nextFloat() < 0.3f) {
                sendHurtReaction(player);
            }
        } else {
            if (nonStaserRetaliationCooldown <= 0) {
                nonStaserRetaliationCooldown = 100;
                executeNonStaserRetaliation(attacker);
            }
        }

        return damaged;
    }

    private void sendHurtReaction(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        int count = personalityConfig.hurtCount;
        if (count <= 0) return;
        String base = "entity.doctor_m.103_tardis.hurt." + personalityConfig.lowerName;
        player.sendMessage(Text.translatable(base + "." + random.nextInt(count), getTardisName()), true);
    }

    private void requestCeaseFire(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        int count = personalityConfig.ceasefireCount;
        if (count <= 0) return;
        String base = "entity.doctor_m.103_tardis.ceasefire." + personalityConfig.lowerName;
        player.sendMessage(Text.translatable(base + "." + random.nextInt(count), getTardisName()), false);
    }

    private void warnBeforeRetaliation(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        if (hasWarnedCurrentAggressor) return;
        hasWarnedCurrentAggressor = true;
        int count = personalityConfig.warnCount;
        if (count <= 0) return;
        String base = "entity.doctor_m.103_tardis.warn." + personalityConfig.lowerName;
        player.sendMessage(Text.translatable(base + "." + random.nextInt(count), getTardisName()), false);
    }

    // ==================== 非 Staser 反击 ====================
    private void executeNonStaserRetaliation(LivingEntity attacker) {
        if (this.getWorld().isClient) return;

        Map<RetaliateType, Integer> weights = new EnumMap<>(RetaliateType.class);
        for (Map.Entry<RetaliateType, Integer> entry : RETALIATE_WEIGHTS.get(personality).entrySet()) {
            if (entry.getKey() != RetaliateType.ENERGY_BEAM) {
                weights.put(entry.getKey(), entry.getValue());
            }
        }
        PersonalityConfig tempConfig = new PersonalityConfig(personality, weights);
        RetaliateType chosen = weightedRandom(tempConfig);

        spawnRetaliateParticles();
        switch (chosen) {
            case MELEE -> retaliateMelee(attacker);
            case HIGH_ALTITUDE -> retaliateHighAltitude(attacker);
            case TELEPORT_TRENZALORE -> retaliateTeleportTrenzalore(attacker);
            case TELEPORT_VORTEX -> retaliateTeleportVortex(attacker);
            default -> retaliateMelee(attacker);
        }
    }

    private void retaliateMelee(LivingEntity attacker) {
        this.tryAttack(attacker);
        this.swingHand(Hand.MAIN_HAND);
        if (attacker instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("entity.doctor_m.103_tardis.retaliate.melee." + random.nextInt(2), getTardisName()), true);
        }
    }

    private void retaliateHighAltitude(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player) {
            double targetY = player.getY() + 60 + random.nextInt(40);
            player.teleport(player.getServerWorld(), player.getX(), targetY, player.getZ(), player.getYaw(), player.getPitch());
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0));
            player.sendMessage(Text.translatable("entity.doctor_m.103_tardis.retaliate.high_altitude." + random.nextInt(2), getTardisName()), true);
        } else {
            attacker.damage(this.getWorld().getDamageSources().fall(), 8.0f);
        }
    }

    private void retaliateTeleportTrenzalore(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        RegistryKey<World> targetDim = RegistryKey.of(RegistryKeys.WORLD, new Identifier("doctor_m", "trenzalore"));
        ServerWorld targetWorld = player.getServer().getWorld(targetDim);
        if (targetWorld == null) return;
        BlockPos safePos = findSafePos(targetWorld, 0, 65, 0, 10);
        player.teleport(targetWorld, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                player.getYaw(), player.getPitch());
        player.sendMessage(Text.translatable("entity.doctor_m.103_tardis.retaliate.trenzalore." + random.nextInt(2), getTardisName()), true);
    }

    private void retaliateTeleportVortex(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player) {
            RegistryKey<World> vortexDim = RegistryKey.of(RegistryKeys.WORLD, new Identifier("ait", "time_vortex"));
            ServerWorld vortexWorld = player.getServer().getWorld(vortexDim);
            if (vortexWorld != null) {
                player.teleport(vortexWorld, player.getX(), 300, player.getZ(), player.getYaw(), player.getPitch());
            }
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
            player.sendMessage(Text.translatable("entity.doctor_m.103_tardis.retaliate.vortex." + random.nextInt(2), getTardisName()), true);
        } else {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1));
        }
    }

    // ==================== 发射 Staser Bolt ====================
    public void shootStaserBolt(LivingEntity target) {
        if (this.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) this.getWorld();
        ItemStack boltStack = GunItems.STASER_BOLT_MAGAZINE.getDefaultStack();
        StaserBoltMagazine boltItem = (StaserBoltMagazine) boltStack.getItem();
        PersistentProjectileEntity projectile = boltItem.createStaserbolt(world, boltStack, this);
        projectile.setPos(this.getX(), this.getY() + 1.2, this.getZ());
        Vec3d start = this.getPos().add(0, this.getHeight() * 0.8, 0);
        Vec3d end = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d direction = end.subtract(start).normalize();
        float speed = 4.0f;
        float divergence = 0.2f;
        projectile.setVelocity(direction.x, direction.y, direction.z, speed, divergence);
        projectile.setCritical(true);
        projectile.setShotFromCrossbow(true);
        projectile.setSound(AITSounds.STASER);
        world.spawnEntity(projectile);
        world.playSound(null, this.getBlockPos(), AITSounds.STASER, SoundCategory.HOSTILE, 0.25f, 1.0f);
    }

    // ==================== 交互系统 ====================
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) return ActionResult.SUCCESS;

        String name = getTardisName();
        if (personality == Personality.TRADER) {
            if (player.isSneaking()) {
                tryTrade((ServerPlayerEntity) player, name);
            } else {
                player.sendMessage(Text.translatable("entity.doctor_m.103_tardis.welcome." + random.nextInt(4), name), false);
                player.sendMessage(Text.translatable("entity.doctor_m.103_tardis.trade.hint.subsystem"), false);
                sendTradeList((ServerPlayerEntity) player, name);
                player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.hint.sneak"), false);
                setState(AIState.TRADING);
            }
        } else {
            int count = personalityConfig.interactCount;
            if (count > 0) {
                String base = "entity.doctor_m.103_tardis.interact." + personalityConfig.lowerName;
                player.sendMessage(Text.translatable(base + "." + random.nextInt(count), name), true);
            }
        }
        return ActionResult.SUCCESS;
    }

    // ==================== 交易辅助 ====================
    private void sendTradeList(ServerPlayerEntity player, String name) {
        if (dailyTrades.isEmpty()) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.empty", name), false);
            return;
        }
        player.sendMessage(Text.literal("§7════════════════════════"), false);
        for (int i = 0; i < dailyTrades.size(); i++) {
            TradeOffer offer = dailyTrades.get(i);
            String status = offer.isAvailable() ? "§e" : "§7§m";
            player.sendMessage(Text.literal(status + "[" + (i + 1) + "] " + offer.getDisplayText()), false);
        }
        player.sendMessage(Text.literal("§7════════════════════════"), false);
    }

    private void tryTrade(ServerPlayerEntity player, String name) {
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.no_item"), false);
            return;
        }

        List<TradeOffer> matches = new ArrayList<>();
        for (TradeOffer offer : dailyTrades) {
            if (offer.isAvailable() && offer.getInputItem() == held.getItem()) {
                matches.add(offer);
            }
        }

        if (matches.isEmpty()) {
            player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.reject"), false);
            return;
        }

        matches.sort((a, b) -> Integer.compare(b.getInputCount(), a.getInputCount()));

        int heldCount = held.getCount();
        for (TradeOffer offer : matches) {
            if (heldCount >= offer.getInputCount()) {
                offer.execute(player);
                player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.success", name), false);
                grantTradeAdvancement(player);
                return;
            }
        }

        player.sendMessage(Text.translatable("doctor_m.dialog.common.trade.insufficient"), false);
    }

    private void grantTradeAdvancement(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        var advancement = server.getAdvancementLoader().get(new Identifier("doctor_m", "cross_time_trade"));
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "impossible");
        }
    }

    // ==================== 皮肤系统 ====================
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SELECTED_SKIN, "");
        this.dataTracker.startTracking(MODEL_TYPE, "slim");
        this.dataTracker.startTracking(CURRENT_STATE, AIState.IDLE.id);
    }

    private void chooseRandomSkin() {
        List<SkinEntry> entries = loadSkinList();
        if (entries.isEmpty()) {
            this.displayName = "Type103";
            this.selectedSkin = "default.png";
            this.modelType = "slim";
            this.setCustomName(Text.literal(this.displayName));
            this.setCustomNameVisible(true);
            this.dataTracker.set(SELECTED_SKIN, this.selectedSkin);
            this.dataTracker.set(MODEL_TYPE, this.modelType);
            return;
        }
        SkinEntry chosen = entries.get(this.random.nextInt(entries.size()));
        this.selectedSkin = chosen.texture;
        this.displayName = chosen.display;
        this.modelType = chosen.modelType;
        this.setCustomName(Text.literal(displayName));
        this.setCustomNameVisible(true);
        this.dataTracker.set(SELECTED_SKIN, selectedSkin);
        this.dataTracker.set(MODEL_TYPE, modelType);
    }

    private List<SkinEntry> loadSkinList() {
        List<SkinEntry> list = new ArrayList<>();
        try (InputStream stream = Entity103Tardis.class.getResourceAsStream("/assets/doctor_m/textures/entity/tardis/skins.txt")) {
            if (stream == null) {
                System.err.println("[Entity103Tardis] Could not find skins.txt");
                return list;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                String textureName;
                String displayName;
                String modelType = "slim";
                if (parts.length >= 2) {
                    textureName = parts[0].trim();
                    displayName = parts[1].trim();
                    if (parts.length >= 3) {
                        String type = parts[2].trim().toLowerCase();
                        modelType = (type.equals("default") || type.equals("steve")) ? "default" : "slim";
                    }
                } else {
                    textureName = line;
                    String nameWithoutExt = line.contains(".") ? line.substring(0, line.lastIndexOf('.')) : line;
                    displayName = nameWithoutExt;
                }
                textureName = SKIN_NAME_PATTERN.matcher(textureName.toLowerCase()).replaceAll("_");
                if (!textureName.endsWith(".png")) textureName += ".png";
                list.add(new SkinEntry(textureName, displayName, modelType));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static class SkinEntry {
        String texture, display, modelType;
        SkinEntry(String t, String d, String m) { texture = t; display = d; modelType = m; }
    }

    public String getSelectedSkin() {
        return this.getWorld().isClient ? this.dataTracker.get(SELECTED_SKIN) : selectedSkin;
    }

    public String getModelType() {
        return this.getWorld().isClient ? this.dataTracker.get(MODEL_TYPE) : modelType;
    }

    public String getTardisName() {
        String name = this.getWorld().isClient
                ? (this.getCustomName() != null ? this.getCustomName().getString() : displayName)
                : displayName;
        return name == null || name.isEmpty() ? "Type-103" : name;
    }

    // ==================== 状态管理 ====================
    public void setState(AIState state) {
        if (!this.getWorld().isClient) {
            this.dataTracker.set(CURRENT_STATE, state.id);
        }
    }

    public AIState getState() {
        return AIState.values()[this.dataTracker.get(CURRENT_STATE)];
    }

    public Personality getPersonality() {
        return personality;
    }

    // ==================== NBT 序列化 ====================
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SelectedSkin", selectedSkin);
        nbt.putString("DisplayName", displayName);
        nbt.putString("ModelType", modelType);
        nbt.putString("Personality", personality.name());
        if (lastAggressorUUID != null) {
            nbt.putUuid("LastAggressor", lastAggressorUUID);
        }
        nbt.putLong("LastAggressionTime", lastAggressionTime);
        nbt.putBoolean("HasWarned", hasWarnedCurrentAggressor);
        nbt.putInt("AggressionCount", aggressionCount);
        nbt.putLong("LastTradeRefreshDay", lastTradeRefreshDay);
        if (!dailyTrades.isEmpty()) {
            nbt.put("DailyTrades", TradeManager.writeOffersToNbt(dailyTrades));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SelectedSkin")) {
            selectedSkin = nbt.getString("SelectedSkin");
            displayName = nbt.getString("DisplayName");
            modelType = nbt.getString("ModelType");
            this.dataTracker.set(SELECTED_SKIN, selectedSkin);
            this.dataTracker.set(MODEL_TYPE, modelType);
            this.setCustomName(Text.literal(displayName));
            this.setCustomNameVisible(true);
        }
        if (nbt.contains("Personality")) {
            try {
                this.personality = Personality.valueOf(nbt.getString("Personality"));
            } catch (IllegalArgumentException ignored) {
                this.personality = Personality.TRADER;
            }
            this.personalityConfig = PERSONALITY_CONFIGS.get(this.personality);
        }
        if (nbt.contains("LastAggressor")) {
            lastAggressorUUID = nbt.getUuid("LastAggressor");
        }
        lastAggressionTime = nbt.getLong("LastAggressionTime");
        hasWarnedCurrentAggressor = nbt.getBoolean("HasWarned");
        aggressionCount = nbt.contains("AggressionCount") ? nbt.getInt("AggressionCount") : 0;
        lastTradeRefreshDay = nbt.contains("LastTradeRefreshDay") ? nbt.getLong("LastTradeRefreshDay") : -1;
        if (nbt.contains("DailyTrades", 9)) {
            dailyTrades = TradeManager.readOffersFromNbt(nbt.getList("DailyTrades", 10));
        }

        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);
        this.initGoals();
    }

    // ==================== 属性 ====================
    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    // ==================== 辅助方法 ====================
    private void spawnRetaliateParticles() {
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1, this.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
            sw.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.HOSTILE, 1.0f, 1.2f);
        }
    }

    private BlockPos findSafePos(ServerWorld world, int centerX, int centerY, int centerZ, int radius) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        mutable.set(centerX, centerY, centerZ);
        if (isSafe(world, mutable)) return mutable.toImmutable();

        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                mutable.set(centerX + dx, centerY, centerZ - r);
                if (isSafe(world, mutable)) return mutable.toImmutable();
                mutable.set(centerX + dx, centerY, centerZ + r);
                if (isSafe(world, mutable)) return mutable.toImmutable();
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                mutable.set(centerX - r, centerY, centerZ + dz);
                if (isSafe(world, mutable)) return mutable.toImmutable();
                mutable.set(centerX + r, centerY, centerZ + dz);
                if (isSafe(world, mutable)) return mutable.toImmutable();
            }
        }
        return new BlockPos(centerX, centerY, centerZ);
    }

    private boolean isSafe(ServerWorld world, BlockPos.Mutable pos) {
        return world.getBlockState(pos).isAir()
                && world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
    }

    private RetaliateType weightedRandom(PersonalityConfig config) {
        int total = config.weightPrefixSum[config.weightPrefixSum.length - 1];
        if (total <= 0) return RetaliateType.MELEE;
        int roll = this.random.nextInt(total);
        int lo = 0, hi = config.weightPrefixSum.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (config.weightPrefixSum[mid] <= roll) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return config.weightedTypes[lo];
    }
}