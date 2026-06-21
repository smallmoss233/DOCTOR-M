package doctor_m.module.creativity.handler;

import doctor_m.module.creativity.creativity_data.tlipoca_scythe;
import doctor_m.util.ScytheGrowthManager;
import doctor_m.util.ScytheSlashManager;
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

    // 【修改】100 ticks = 5秒检测一次
    private static final int CHECK_INTERVAL = 100;

    // 【修改】使用 -1 表示无限时间
    private static final int INFINITE_DURATION = -1;

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

    private static final Map<UUID, Boolean> playerHasScythe = new HashMap<>();
    private static final Map<UUID, Boolean> playerWasHighHealth = new HashMap<>();

    // 记录玩家最后一次用镰刀攻击时的武器栈
    private static final Map<UUID, ItemStack> lastKillWeapon = new HashMap<>();

    public static void register() {

        // ===== 1. 被动效果 + 生命加成 =====
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % CHECK_INTERVAL != 0) return;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack scytheStack = findScytheInInventory(player);
                boolean hasScythe = scytheStack != null;
                boolean hadScythe = playerHasScythe.getOrDefault(player.getUuid(), false);

                if (hasScythe && !hadScythe) {
                    // 刚拿到镰刀
                    tlipoca_scythe.applyMaxHealthBoost(player);
                    float healthPercent = player.getHealth() / player.getMaxHealth();
                    boolean isHighHealth = healthPercent > 0.5f;
                    applyPassiveEffects(player, isHighHealth);
                    playerWasHighHealth.put(player.getUuid(), isHighHealth);
                } else if (!hasScythe && hadScythe) {
                    // 刚失去镰刀
                    tlipoca_scythe.removeMaxHealthBoost(player);
                    removeAllTlipocaEffects(player);
                    playerWasHighHealth.remove(player.getUuid());
                }

                if (hasScythe) {
                    float healthPercent = player.getHealth() / player.getMaxHealth();
                    boolean isHighHealth = healthPercent > 0.5f;
                    boolean wasHighHealth = playerWasHighHealth.getOrDefault(player.getUuid(), isHighHealth);

                    if (isHighHealth != wasHighHealth || !playerWasHighHealth.containsKey(player.getUuid())) {
                        applyPassiveEffects(player, isHighHealth);
                        playerWasHighHealth.put(player.getUuid(), isHighHealth);
                    } else {
                        ensurePassiveEffects(player, isHighHealth);
                    }
                }

                playerHasScythe.put(player.getUuid(), hasScythe);
            }
        });

        // ===== 2. 攻击时给目标上负面效果 + 记录武器 =====
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof PlayerEntity player) {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof tlipoca_scythe) {
                    // 【关键】记录这把武器，用于死亡时的成长判定
                    lastKillWeapon.put(player.getUuid(), stack);

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

        // ===== 3. 击杀成长 =====
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            // 尝试获取玩家
            PlayerEntity attacker = null;
            if (source.getAttacker() instanceof PlayerEntity p) {
                attacker = p;
            } else if (source.getSource() instanceof PlayerEntity p) {
                attacker = p;
            }

            if (attacker instanceof ServerPlayerEntity player) {
                // 【关键】优先使用ALLOW_DAMAGE中记录的武器
                ItemStack weapon = lastKillWeapon.get(player.getUuid());

                // 如果没有记录，尝试从背包找
                if (weapon == null || !(weapon.getItem() instanceof tlipoca_scythe)) {
                    weapon = findScytheInInventory(player);
                }

                if (weapon != null && weapon.getItem() instanceof tlipoca_scythe) {
                    // 调用成长工具类
                    ScytheGrowthManager.onKill(weapon, player);
                }

                // 清除记录
                lastKillWeapon.remove(player.getUuid());
            }
        });

        // ===== 4. 左键攻击触发斩击 =====
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof tlipoca_scythe)) {
                return ActionResult.PASS;
            }

            if (!world.isClient) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    if (!ScytheSlashManager.isOnCooldown(world, player)) {
                        ScytheSlashManager.performSlash(
                                (net.minecraft.server.world.ServerWorld) world,
                                serverPlayer,
                                stack
                        );
                        ScytheSlashManager.setCooldown(world, player);
                    }
                }
            } else {
                if (!player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                    ScytheSlashManager.spawnParticlesClient(player);
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

    // ========== 被动效果 ==========

    private static void applyPassiveEffects(ServerPlayerEntity player, boolean isHighHealth) {
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.REGENERATION);

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, INFINITE_DURATION, 4, false, false));

        if (isHighHealth) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, INFINITE_DURATION, 4, false, false));
        } else {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, INFINITE_DURATION, 4, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, INFINITE_DURATION, 4, false, false));
        }
    }

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

    private static void ensureEffect(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffect effect, int amplifier) {
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