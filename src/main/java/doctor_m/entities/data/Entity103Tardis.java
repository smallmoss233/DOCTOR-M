package doctor_m.entities.data;

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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
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
    private long lastRetaliateTime = 0;
    private static final int RETALIATE_COOLDOWN = 30;
    private UUID lastAggressorUUID = null;
    private long lastAggressionTime = 0;
    private static final long AGGRESSION_MEMORY = 600L;
    private boolean hasWarnedCurrentAggressor = false;
    private int aggressionCount = 0;

    // ==================== 交易系统（新增）====================
    private List<TradeOffer> dailyTrades = new ArrayList<>();
    private long lastTradeRefreshDay = -1;
    private static final String TRADE_POOL_FILE = "trades_103.json";

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
    }

    // ==================== 构造 ====================
    public Entity103Tardis(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        if (!world.isClient) {
            if (this.personality == Personality.TRADER) {
                Personality[] values = Personality.values();
                float roll = this.random.nextFloat();
                if (roll < 0.60f) {
                    this.personality = Personality.TRADER;
                } else {
                    Personality chosen;
                    do {
                        chosen = values[this.random.nextInt(values.length)];
                    } while (chosen == Personality.TRADER);
                    this.personality = chosen;
                }
            }
            chooseRandomSkin();
            setState(AIState.IDLE);
        }
    }

    // ==================== AI 初始化 ====================
    @Override
    protected void initGoals() {
        super.initGoals();
        if (this.personality == null) {
            this.personality = Personality.TRADER;
        }
        this.goalSelector.add(0, new SwimGoal(this));
        initCombatGoals();
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));
        initMovementGoals();
    }

    private void initCombatGoals() {
        switch (personality) {
            case AGGRESSIVE:
                this.targetSelector.add(1, new RevengeGoal(this));
                this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
                this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, true));
                break;
            case BRAVE:
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, true));
                this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, true));
                break;
            case DEFENSIVE:
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                break;
            case TIMID:
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 12.0f, 1.0, 1.4));
                break;
            case TRADER:
                this.targetSelector.add(1, new RevengeGoal(this, PlayerEntity.class));
                break;
        }
    }

    private void initMovementGoals() {
        switch (personality) {
            case AGGRESSIVE:
                this.goalSelector.add(5, new WanderAroundGoal(this, 0.7));
                this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.6));
                break;
            case BRAVE:
                this.goalSelector.add(5, new WanderAroundGoal(this, 0.8));
                break;
            case DEFENSIVE:
                this.goalSelector.add(5, new WanderAroundGoal(this, 0.5));
                break;
            case TIMID:
                this.goalSelector.add(5, new WanderAroundGoal(this, 0.4));
                break;
            case TRADER:
                this.goalSelector.add(5, new WanderAroundGoal(this, 0.55));
                this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.5));
                break;
        }
    }

    // ==================== Tick（新增交易刷新）====================
    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld sw) {
            long currentDay = sw.getTime() / 24000L;
            if (currentDay > lastTradeRefreshDay || dailyTrades.isEmpty()) {
                refreshTrades(sw.getServer());
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
        boolean damaged = super.damage(source, amount);
        if (!damaged || this.getWorld().isClient) return damaged;
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

            if (personality == Personality.AGGRESSIVE) {
                if (now - lastRetaliateTime >= RETALIATE_COOLDOWN) {
                    lastRetaliateTime = now;
                    executeRetaliation(attacker);
                }
            } else {
                requestCeaseFire(attacker);
            }
            if (this.random.nextFloat() < 0.3f) {
                sendHurtReaction(attacker);
            }
            return damaged;
        }

        if (now - lastRetaliateTime < RETALIATE_COOLDOWN) return damaged;
        lastRetaliateTime = now;
        lastAggressionTime = now;
        aggressionCount++;

        switch (personality) {
            case AGGRESSIVE -> executeRetaliation(attacker);
            case BRAVE -> {
                if (aggressionCount == 2) warnBeforeRetaliation(attacker);
                else executeRetaliation(attacker);
            }
            case DEFENSIVE, TIMID, TRADER -> {
                if (aggressionCount == 2) warnBeforeRetaliation(attacker);
                else if (aggressionCount >= 3) executeRetaliation(attacker);
            }
        }
        return damaged;
    }

    private void sendHurtReaction(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        String name = getTardisName();
        String[] msgs = switch (personality) {
            case AGGRESSIVE -> new String[] {
                    String.format("§c§o[%s] 你会为此付出代价！", name),
                    String.format("§c§o[%s] 找死！", name)
            };
            case BRAVE -> new String[] {
                    String.format("§6§o[%s] 就这点力气？", name),
                    String.format("§6§o[%s] 哼，不过如此。", name)
            };
            case DEFENSIVE -> new String[] {
                    String.format("§e§o[%s] 啊！为什么...", name),
                    String.format("§e§o[%s] 请住手！", name)
            };
            case TIMID -> new String[] {
                    String.format("§e§o[%s] 好痛...呜呜...", name),
                    String.format("§e§o[%s] 救命啊...", name)
            };
            case TRADER -> new String[] {
                    String.format("§c§o[%s] 我的货！你打坏了我的货！", name),
                    String.format("§c§o[%s] 哎哟！这得赔多少钱啊...", name)
            };
        };
        if (msgs.length > 0) {
            player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
        }
    }

    private void requestCeaseFire(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        String name = getTardisName();
        String[] msgs = switch (personality) {
            case DEFENSIVE -> new String[] {
                    String.format("§e§o[%s] 请停手！我不想与你为敌...", name),
                    String.format("§e§o[%s] 我们可以谈谈，暴力解决不了问题...", name)
            };
            case TIMID -> new String[] {
                    String.format("§e§o[%s] 别、别打我！求你了...", name),
                    String.format("§e§o[%s] 我、我只是路过...请不要伤害我...", name)
            };
            case TRADER -> new String[] {
                    String.format("§c§o[%s] 嘿！顾客就是上帝，但上帝也不该打商人啊！", name),
                    String.format("§c§o[%s] 停手！你打坏了我还怎么做生意？", name)
            };
            case BRAVE -> new String[] {
                    String.format("§6§o[%s] 你确定要这么做吗？", name),
                    String.format("§6§o[%s] 我劝你三思，这不是明智之举。", name)
            };
            default -> new String[0];
        };
        if (msgs.length > 0) {
            player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), false);
        }
    }

    private void warnBeforeRetaliation(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        if (hasWarnedCurrentAggressor) return;
        hasWarnedCurrentAggressor = true;
        String name = getTardisName();
        String[] msgs = switch (personality) {
            case DEFENSIVE -> new String[] {
                    String.format("§4§l[%s] §r§c这是最后的警告。再攻击我，你会被扔进时间涡旋！", name),
                    String.format("§4§l[%s] §r§c我的忍耐是有限度的，最后一次机会！", name)
            };
            case TIMID -> new String[] {
                    String.format("§4§l[%s] §r§c我、我不想的...但别逼我！时间涡旋可不是闹着玩的！", name),
                    String.format("§4§l[%s] §r§c请、请住手！我不想伤害任何人，但我会保护自己的...", name)
            };
            case TRADER -> new String[] {
                    String.format("§4§l[%s] §r§c我的耐心是有限的！时间涡旋可不是一个好去处，别逼我！", name),
                    String.format("§4§l[%s] §r§c你毁了一笔潜在的交易！这是最后一次警告！", name)
            };
            case BRAVE -> new String[] {
                    String.format("§4§l[%s] §r§c你已经越界了。下一击，我不会再留情。", name),
                    String.format("§4§l[%s] §r§c我的宽容到此为止。准备好承受后果吧。", name)
            };
            default -> new String[0];
        };
        if (msgs.length > 0) {
            player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), false);
        }
    }

    // ==================== 反击执行 ====================
    private void executeRetaliation(LivingEntity attacker) {
        if (this.getWorld().isClient) return;
        Map<RetaliateType, Integer> weights = RETALIATE_WEIGHTS.getOrDefault(personality, RETALIATE_WEIGHTS.get(Personality.TRADER));
        RetaliateType chosen = weightedRandom(weights);
        this.getWorld().sendEntityStatus(this, (byte) 4);
        spawnRetaliateParticles();
        switch (chosen) {
            case MELEE -> retaliateMelee(attacker);
            case ENERGY_BEAM -> retaliateEnergyBeam(attacker);
            case HIGH_ALTITUDE -> retaliateHighAltitude(attacker);
            case TELEPORT_TRENZALORE -> retaliateTeleportTrenzalore(attacker);
            case TELEPORT_VORTEX -> retaliateTeleportVortex(attacker);
        }
    }

    private void retaliateMelee(LivingEntity attacker) {
        this.tryAttack(attacker);
        if (attacker instanceof ServerPlayerEntity player) {
            String name = getTardisName();
            String[] msgs = {
                    String.format("§c§o%s 狠狠地给了你一拳！", name),
                    String.format("§c§o%s 用尽全力向你挥出一击！", name)
            };
            player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
        }
    }

    private void retaliateEnergyBeam(LivingEntity attacker) {
        float damage = (personality == Personality.AGGRESSIVE || personality == Personality.BRAVE) ? 6.0f : 3.0f;
        attacker.damage(this.getWorld().getDamageSources().magic(), damage);
        if (attacker instanceof ServerPlayerEntity player) {
            String name = getTardisName();
            String[] msgs = {
                    String.format("§b§o你被 %s 的能量束击中了！", name),
                    String.format("§b§o%s 发射了一道耀眼的光束！", name)
            };
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
            player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
        }
        if (this.getWorld() instanceof ServerWorld sw) {
            Vec3d start = this.getPos().add(0, this.getHeight() * 0.8, 0);
            Vec3d end = attacker.getPos().add(0, attacker.getHeight() * 0.5, 0);
            Vec3d dir = end.subtract(start).normalize();
            for (int i = 0; i < 20; i++) {
                Vec3d pos = start.add(dir.multiply(i * 0.5));
                sw.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    private void retaliateHighAltitude(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player) {
            String name = getTardisName();
            double targetY = player.getY() + 60 + random.nextInt(40);
            player.teleport(player.getServerWorld(), player.getX(), targetY, player.getZ(), player.getYaw(), player.getPitch());
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0));
            String[] msgs = {
                    String.format("§d§o你被 %s 抛向了高空...时间能量在你周围涌动！", name),
                    String.format("§d§o%s 发动了重力反转，你感觉自己飞了起来！", name)
            };
            player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
        } else {
            attacker.damage(this.getWorld().getDamageSources().fall(), 8.0f);
        }
    }

    private void retaliateTeleportTrenzalore(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player) {
            String name = getTardisName();
            RegistryKey<World> targetDim = RegistryKey.of(RegistryKeys.WORLD, new Identifier("doctor_m", "trenzalore"));
            ServerWorld targetWorld = player.getServer().getWorld(targetDim);
            if (targetWorld != null) {
                BlockPos safePos = findSafePos(targetWorld, 0, 65, 0, 10);
                player.teleport(targetWorld, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                        player.getYaw(), player.getPitch());
                String[] msgs = {
                        String.format("§4§o你被 %s 传送到了特兰泽洛！这是对你攻击的惩罚。", name),
                        String.format("§4§o%s 打开了时空裂缝，你被吸入了特兰泽洛！", name)
                };
                player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
            }
        }
    }

    private void retaliateTeleportVortex(LivingEntity attacker) {
        if (attacker instanceof ServerPlayerEntity player) {
            String name = getTardisName();
            RegistryKey<World> vortexDim = RegistryKey.of(RegistryKeys.WORLD, new Identifier("ait", "time_vortex"));
            ServerWorld vortexWorld = player.getServer().getWorld(vortexDim);
            if (vortexWorld != null) {
                player.teleport(vortexWorld, player.getX(), 300, player.getZ(), player.getYaw(), player.getPitch());
            }
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
            String[] msgs = {
                    String.format("§4§l%s 将你扔进了时间涡旋！", name),
                    String.format("§4§l时间涡旋在 %s 的召唤下吞噬了你！", name)
            };
            player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
        } else {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1));
        }
    }

    // ==================== 交互系统（已集成交易）====================
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient) {
            String name = getTardisName();
            if (personality == Personality.TRADER) {
                // 商人欢迎语
                String[] welcomeMsgs = {
                        String.format("§a§l[%s] §r§7你想要什么？我这里有泽顿能量、阿特隆水晶...", name),
                        String.format("§a§l[%s] §r§7来看看今天的特价货吧，走过路过不要错过！", name),
                        String.format("§a§l[%s] §r§7我这儿可有不少好东西，你有足够的能量单元吗？", name),
                        String.format("§a§l[%s] §r§7欢迎光临！今天的时间线波动有点大，进货可不容易。", name)
                };
                player.sendMessage(Text.literal(welcomeMsgs[this.random.nextInt(welcomeMsgs.length)]), false);
                player.sendMessage(Text.literal("§8§o（子系统？那得看运气，不是每次都有货。）"), false);

                // 显示交易列表 & 尝试交易
                sendTradeList((ServerPlayerEntity) player, name);
                tryTrade((ServerPlayerEntity) player, name);
                setState(AIState.TRADING);
            } else {
                switch (personality) {
                    case TIMID -> {
                        String[] msgs = {
                                String.format("§e§o%s 紧张地看着你，似乎随时准备逃跑...", name),
                                String.format("§e§o%s 缩了缩肩膀，小声嘀咕着什么...", name),
                                String.format("§e§o%s 小心翼翼地打量着你...", name)
                        };
                        player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
                    }
                    case AGGRESSIVE -> {
                        String[] msgs = {
                                String.format("§c§o%s 警惕地盯着你，最好不要惹她。", name),
                                String.format("§c§o%s 眯起了眼睛，手已经按在了腰间的装置上...", name),
                                String.format("§c§o%s 冷冷地看着你：\"离我远点。\"", name)
                        };
                        player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
                    }
                    case DEFENSIVE -> {
                        String[] msgs = {
                                String.format("§7§o%s 微微后退，保持着礼貌的距离。", name),
                                String.format("§7§o%s 谨慎地注视着你的一举一动...", name),
                                String.format("§7§o%s 没有靠近，但也没有表现出敌意。", name)
                        };
                        player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
                    }
                    case BRAVE -> {
                        String[] msgs = {
                                String.format("§6§o%s 点了点头，目光中带着审视。", name),
                                String.format("§6§o%s 双手抱胸，饶有兴趣地看着你。", name),
                                String.format("§6§o%s 微微一笑：\"想聊聊？我听着呢。\"", name)
                        };
                        player.sendMessage(Text.literal(msgs[this.random.nextInt(msgs.length)]), true);
                    }
                    default -> {}
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    // ==================== 交易辅助方法（新增）====================
    private void sendTradeList(ServerPlayerEntity player, String name) {
        if (dailyTrades.isEmpty()) {
            player.sendMessage(Text.literal("§7§o（" + name + " 今天没有东西可卖。）"), false);
            return;
        }
        player.sendMessage(Text.literal("§7════════════════════════"), false);
        for (int i = 0; i < dailyTrades.size(); i++) {
            TradeOffer offer = dailyTrades.get(i);
            String status = offer.isAvailable() ? "§e" : "§7§m";
            player.sendMessage(Text.literal(status + "[" + (i + 1) + "] " + offer.getDisplayText()), false);
        }
        player.sendMessage(Text.literal("§7════════════════════════"), false);
        player.sendMessage(Text.literal("§7§o手持对应物品右键即可交易"), false);
    }

    private void tryTrade(ServerPlayerEntity player, String name) {
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) return;

        for (TradeOffer offer : dailyTrades) {
            if (offer.matches(held) && offer.isAvailable()) {
                offer.execute(player);
                player.sendMessage(Text.literal("§a§l[" + name + "] §r§a成交！这是你的货。"), false);
                grantTradeAdvancement(player);
                return;
            }
        }
    }

    private void grantTradeAdvancement(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        var advancement = server.getAdvancementLoader().get(new Identifier("doctor_m", "trading/cross_time_trade"));
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
            this.displayName = "103型塔迪斯";
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
                textureName = textureName.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
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

    /**
     * 获取实体当前显示的名字，用于所有对话消息中替换硬编码名称。
     * 注意：不能叫 getDisplayName()，因为 Nameable 接口已占用该方法并返回 Text。
     */
    public String getTardisName() {
        String name = this.getWorld().isClient
                ? (this.getCustomName() != null ? this.getCustomName().getString() : displayName)
                : displayName;
        return name == null || name.isEmpty() ? "103型塔迪斯" : name;
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

    // ==================== NBT 序列化（已包含交易数据）====================
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

        // 交易数据持久化
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
        }
        if (nbt.contains("LastAggressor")) {
            lastAggressorUUID = nbt.getUuid("LastAggressor");
        }
        lastAggressionTime = nbt.getLong("LastAggressionTime");
        hasWarnedCurrentAggressor = nbt.getBoolean("HasWarned");
        aggressionCount = nbt.contains("AggressionCount") ? nbt.getInt("AggressionCount") : 0;

        // 交易数据读取
        lastTradeRefreshDay = nbt.contains("LastTradeRefreshDay") ? nbt.getLong("LastTradeRefreshDay") : -1;
        if (nbt.contains("DailyTrades", 9)) { // 9 = NBT List
            dailyTrades = TradeManager.readOffersFromNbt(nbt.getList("DailyTrades", 10)); // 10 = NBT Compound
        }
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
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    mutable.set(centerX + dx, centerY, centerZ + dz);
                    if (world.getBlockState(mutable).isAir()
                            && world.getBlockState(mutable.down()).isSolidBlock(world, mutable.down())) {
                        return mutable.toImmutable();
                    }
                }
            }
        }
        return new BlockPos(centerX, centerY, centerZ);
    }

    private RetaliateType weightedRandom(Map<RetaliateType, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = this.random.nextInt(total);
        int current = 0;
        for (Map.Entry<RetaliateType, Integer> entry : weights.entrySet()) {
            current += entry.getValue();
            if (roll < current) return entry.getKey();
        }
        return RetaliateType.MELEE;
    }
}