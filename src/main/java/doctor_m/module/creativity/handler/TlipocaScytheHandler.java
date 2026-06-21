package doctor_m.module.creativity.handler;

import doctor_m.module.creativity.creativity_data.tlipoca_scythe;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TlipocaScytheHandler {

    private static final int CHECK_INTERVAL = 40;
    // 用极大值代替 -1，避免客户端崩溃
    private static final int INFINITE_DURATION = 999999;

    // 所有原版负面效果（等级10）
    private static final StatusEffectInstance[] NEGATIVE_EFFECTS = {
            new StatusEffectInstance(StatusEffects.SLOWNESS, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.MINING_FATIGUE, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.WEAKNESS, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.BLINDNESS, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.NAUSEA, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.WITHER, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.POISON, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.HUNGER, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.LEVITATION, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.UNLUCK, INFINITE_DURATION, 9, false, false),
            new StatusEffectInstance(StatusEffects.DARKNESS, INFINITE_DURATION, 9, false, false)
    };

    // 记录玩家上一 tick 是否持有镰刀
    private static final Map<UUID, Boolean> playerHasScythe = new HashMap<>();
    // 记录玩家上一 tick 的血量状态（高/低），避免临界波动
    private static final Map<UUID, Boolean> playerWasHighHealth = new HashMap<>();

    public static void register() {
        // 1. 被动效果 + 生命加成
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % CHECK_INTERVAL != 0) return;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack scytheStack = findScytheInInventory(player);
                boolean hasScythe = scytheStack != null;
                boolean hadScythe = playerHasScythe.getOrDefault(player.getUuid(), false);

                if (hasScythe && !hadScythe) {
                    // 刚拿到镰刀：加生命上限
                    tlipoca_scythe.applyMaxHealthBoost(player);
                    // 立即应用被动效果
                    float healthPercent = player.getHealth() / player.getMaxHealth();
                    boolean isHighHealth = healthPercent > 0.5f;
                    applyPassiveEffects(player, isHighHealth);
                    playerWasHighHealth.put(player.getUuid(), isHighHealth);
                } else if (!hasScythe && hadScythe) {
                    // 刚失去镰刀：清生命加成 + 清效果
                    tlipoca_scythe.removeMaxHealthBoost(player);
                    removeAllTlipocaEffects(player);
                    playerWasHighHealth.remove(player.getUuid());
                }

                if (hasScythe) {
                    float healthPercent = player.getHealth() / player.getMaxHealth();
                    boolean isHighHealth = healthPercent > 0.5f;
                    boolean wasHighHealth = playerWasHighHealth.getOrDefault(player.getUuid(), isHighHealth);

                    // 只有血量状态真正变化时才切换效果，避免临界波动
                    if (isHighHealth != wasHighHealth || !playerWasHighHealth.containsKey(player.getUuid())) {
                        applyPassiveEffects(player, isHighHealth);
                        playerWasHighHealth.put(player.getUuid(), isHighHealth);
                    }
                    // 如果状态没变，检查效果是否还在（可能被牛奶清除了），缺失则补
                    else {
                        ensurePassiveEffects(player, isHighHealth);
                    }
                }

                playerHasScythe.put(player.getUuid(), hasScythe);
            }
        });

        // 2. 攻击时给目标上所有原版负面效果
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof PlayerEntity player) {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof tlipoca_scythe) {
                    if (entity instanceof LivingEntity target) {
                        for (StatusEffectInstance effect : NEGATIVE_EFFECTS) {
                            target.addStatusEffect(new StatusEffectInstance(
                                    effect.getEffectType(),
                                    INFINITE_DURATION,
                                    effect.getAmplifier(),
                                    effect.isAmbient(),
                                    effect.shouldShowParticles()
                            ));
                        }
                    }
                }
            }
            return true; // 允许伤害继续处理
        });

        // 3. 击杀成长
        // 【关键修复】使用AFTER_DEATH事件，通过source获取武器
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity player) {
                // 获取造成伤害时的武器（优先主手，因为攻击通常用主手）
                ItemStack weapon = source.getSource() instanceof PlayerEntity ?
                        player.getMainHandStack() : findScytheInInventory(player);

                // 如果主手是镰刀，直接用它
                if (player.getMainHandStack().getItem() instanceof tlipoca_scythe) {
                    weapon = player.getMainHandStack();
                } else {
                    // 否则在背包中查找
                    weapon = findScytheInInventory(player);
                }

                if (weapon != null && weapon.getItem() instanceof tlipoca_scythe) {
                    tlipoca_scythe.onKill(weapon, player);
                }
            }
        });

        // 4. 左键攻击触发斩击（与右键use不冲突，这里提供另一种触发方式）
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof tlipoca_scythe)) {
                return ActionResult.PASS;
            }

            // 只在服务端执行斩击逻辑
            if (!world.isClient) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    if (!tlipoca_scythe.isOnCooldown(world, player)) {
                        tlipoca_scythe.performSlashAttack(world, serverPlayer, stack);
                        tlipoca_scythe.setCooldown(world, player, (tlipoca_scythe) stack.getItem());
                    }
                }
            } else {
                // 客户端只生成粒子
                if (!player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                    tlipoca_scythe.spawnSlashParticles(player);
                }
            }

            // 返回PASS让原版攻击逻辑继续执行（即正常造成伤害）
            return ActionResult.PASS;
        });
    }

    /**
     * 在玩家背包中查找镰刀（包括主手、副手、物品栏）
     */
    private static ItemStack findScytheInInventory(ServerPlayerEntity player) {
        // 检查主手
        if (player.getMainHandStack().getItem() instanceof tlipoca_scythe) {
            return player.getMainHandStack();
        }
        // 检查副手
        if (player.getOffHandStack().getItem() instanceof tlipoca_scythe) {
            return player.getOffHandStack();
        }
        // 检查物品栏
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof tlipoca_scythe) {
                return stack;
            }
        }
        return null;
    }

    // ========== 被动效果管理 ==========

    /**
     * 切换效果组（高血量/低血量）
     * 先清除所有可能的效果，再应用新的
     */
    private static void applyPassiveEffects(ServerPlayerEntity player, boolean isHighHealth) {
        // 先清除所有可能的效果，避免叠加
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.REGENERATION);

        // 力量5 - 两组都有
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, INFINITE_DURATION, 4, false, false));

        if (isHighHealth) {
            // 高血量：幸运5
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, INFINITE_DURATION, 4, false, false));
        } else {
            // 低血量：抗性提升5 + 生命恢复5
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, INFINITE_DURATION, 4, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, INFINITE_DURATION, 4, false, false));
        }
    }

    /**
     * 确保效果还在（防止被牛奶等清除）
     */
    private static void ensurePassiveEffects(ServerPlayerEntity player, boolean isHighHealth) {
        // 力量5始终要有
        ensureEffect(player, StatusEffects.STRENGTH, 4);

        if (isHighHealth) {
            ensureEffect(player, StatusEffects.LUCK, 4);
            // 移除低血量效果
            player.removeStatusEffect(StatusEffects.RESISTANCE);
            player.removeStatusEffect(StatusEffects.REGENERATION);
        } else {
            ensureEffect(player, StatusEffects.RESISTANCE, 4);
            ensureEffect(player, StatusEffects.REGENERATION, 4);
            // 移除高血量效果
            player.removeStatusEffect(StatusEffects.LUCK);
        }
    }

    private static void ensureEffect(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffect effect, int amplifier) {
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current == null || current.getAmplifier() != amplifier) {
            player.addStatusEffect(new StatusEffectInstance(effect, INFINITE_DURATION, amplifier, false, false));
        }
    }

    /**
     * 移除所有镰刀相关的被动效果
     */
    private static void removeAllTlipocaEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.REGENERATION);
    }
}