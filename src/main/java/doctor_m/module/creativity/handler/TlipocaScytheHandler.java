package doctor_m.module.creativity.handler;

import doctor_m.module.creativity.creativity_data.tlipoca_scythe;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
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

    private static final int CHECK_INTERVAL = 20;
    // 用极大值代替 -1，避免客户端崩溃
    private static final int INFINITE_DURATION = 999999;

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
                    // 刚拿到镰刀：加生命
                    tlipoca_scythe.applyMaxHealthBoost(player);
                } else if (!hasScythe && hadScythe) {
                    // 刚失去镰刀：清生命 + 清效果
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
            return true;
        });

        // 3. 击杀成长
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity player) {
                ItemStack stack = findScytheInInventory(player);
                if (stack != null && stack.getItem() instanceof tlipoca_scythe) {
                    tlipoca_scythe.onKill(stack, player);
                }
            }
        });

        // 4. 左键攻击触发斩击
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof tlipoca_scythe)) {
                return ActionResult.PASS;
            }

            if (world.isClient) {
                if (!player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                    tlipoca_scythe.spawnSlashParticles(player);
                }
            } else {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    if (!tlipoca_scythe.isOnCooldown(world, player)) {
                        tlipoca_scythe.performSlashAttack(world, serverPlayer, stack);
                        tlipoca_scythe.setCooldown(world, player, (tlipoca_scythe) stack.getItem());
                    }
                }
            }

            return ActionResult.PASS;
        });
    }

    private static ItemStack findScytheInInventory(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof tlipoca_scythe) {
            return player.getMainHandStack();
        }
        if (player.getOffHandStack().getItem() instanceof tlipoca_scythe) {
            return player.getOffHandStack();
        }
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof tlipoca_scythe) {
                return stack;
            }
        }
        return null;
    }

    // 切换效果组（高血量/低血量）
    private static void applyPassiveEffects(ServerPlayerEntity player, boolean isHighHealth) {
        // 先清除所有可能的效果
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.REGENERATION);

        // 力量5 两组都有
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, INFINITE_DURATION, 4, false, false));

        if (isHighHealth) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, INFINITE_DURATION, 4, false, false));
        } else {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, INFINITE_DURATION, 4, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, INFINITE_DURATION, 4, false, false));
        }
    }

    // 确保效果还在（防止被牛奶等清除）
    private static void ensurePassiveEffects(ServerPlayerEntity player, boolean isHighHealth) {
        ensureEffect(player, StatusEffects.STRENGTH, 4);
        if (isHighHealth) {
            ensureEffect(player, StatusEffects.LUCK, 4);
            player.removeStatusEffect(StatusEffects.RESISTANCE);
            player.removeStatusEffect(StatusEffects.REGENERATION);
        } else {
            ensureEffect(player, StatusEffects.RESISTANCE, 4);
            ensureEffect(player, StatusEffects.REGENERATION, 4);
            player.removeStatusEffect(StatusEffects.LUCK);
        }
    }

    private static void ensureEffect(ServerPlayerEntity player, StatusEffect effect, int amplifier) {
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current == null || current.getAmplifier() != amplifier) {
            player.addStatusEffect(new StatusEffectInstance(effect, INFINITE_DURATION, amplifier, false, false));
        }
    }

    private static void removeAllTlipocaEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.REGENERATION);
    }
}