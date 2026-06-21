package doctor_m.module.creativity.handler;

import doctor_m.module.creativity.creativity_data.tlipoca_scythe;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class TlipocaScytheHandler {

    private static final int CHECK_INTERVAL = 20;

    private static final StatusEffectInstance[] NEGATIVE_EFFECTS = {
            new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.MINING_FATIGUE, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.WEAKNESS, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.BLINDNESS, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.NAUSEA, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.WITHER, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.POISON, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.HUNGER, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.LEVITATION, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.UNLUCK, -1, 9, false, false),
            new StatusEffectInstance(StatusEffects.DARKNESS, -1, 9, false, false)
    };

    public static void register() {
        // 1. 被动效果 + 生命加成
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % CHECK_INTERVAL != 0) return;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack scytheStack = findScytheInInventory(player);
                if (scytheStack == null) {
                    tlipoca_scythe.removeMaxHealthBoost(player);
                    removeAllTlipocaEffects(player);
                    continue;
                }

                tlipoca_scythe.initScytheNbt(scytheStack);
                tlipoca_scythe.applyMaxHealthBoost(player);

                float healthPercent = player.getHealth() / player.getMaxHealth();
                applyPassiveEffects(player, healthPercent);
            }
        });

        // 2. 攻击时施加负面效果 + 修改伤害（使用 ALLOW_DAMAGE + 手动处理）
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof PlayerEntity player) {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof tlipoca_scythe) {
                    tlipoca_scythe.initScytheNbt(stack);

                    if (entity instanceof LivingEntity target) {
                        for (StatusEffectInstance effect : NEGATIVE_EFFECTS) {
                            target.addStatusEffect(new StatusEffectInstance(
                                    effect.getEffectType(),
                                    -1,
                                    effect.getAmplifier(),
                                    effect.isAmbient(),
                                    effect.shouldShowParticles()
                            ));
                        }
                    }

                    // 由于 ALLOW_DAMAGE 不能修改伤害值，我们返回 false 阻止原伤害，并手动施加新伤害
                    // 注意：需要防止递归，使用标志
                    return false; // 阻止原伤害
                    // 在外部我们通过一个单独的监听器来施加自定义伤害
                }
            }
            return true;
        });

        // 伤害修改：使用另一个事件或监听器来施加自定义伤害（此处使用 ServerLivingEntityEvents.ALLOW_DAMAGE 的替代方案）
        // 由于 ALLOW_DAMAGE 不能改伤害值，我们用 EntityHurtEvents.BEFORE（如果存在）或直接施加。
        // 为了兼容，我们使用一个 Mixin？不，我们用简单方法：在 AFTER_DEATH 前？但需要在伤害计算前修改。
        // 下面用反射或工厂？为了简化，我们改用服务器端事件监听攻击前修改伤害？
        // 实际上，我们可以在攻击时直接调用 entity.damage，但会导致无限递归。所以需要使用 ThreadLocal 标志。
        // 但由于代码已包含，我们直接在 ALLOW_DAMAGE 中用标志进行手动伤害，但需要在事件中避免递归。
        // 更好的方式：使用 EntityHurtEvents.BEFORE 如果可用。但你已经说找不到，所以我们用另一种方法：
        // 我们注册一个 ServerLivingEntityEvents.ALLOW_DAMAGE，在其中手动伤害并返回 false，但加标志防止递归。
        // 同时，为了确保伤害修改，我们需要在 ALLOW_DAMAGE 中判断是否为镰刀，若是则手动伤害并取消原伤害。
        // 下面实现这个逻辑。

        // 修正：由于 ALLOW_DAMAGE 无法修改伤害，我们用下面的方法：
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof PlayerEntity player) {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof tlipoca_scythe) {
                    // 防止递归
                    if (isCustomDamage.get()) {
                        isCustomDamage.set(false);
                        return true;
                    }
                    // 计算自定义伤害
                    float damage = tlipoca_scythe.getTotalAttackDamage(stack);
                    // 施加负面效果
                    if (entity instanceof LivingEntity target) {
                        for (StatusEffectInstance effect : NEGATIVE_EFFECTS) {
                            target.addStatusEffect(new StatusEffectInstance(
                                    effect.getEffectType(),
                                    -1,
                                    effect.getAmplifier(),
                                    effect.isAmbient(),
                                    effect.shouldShowParticles()
                            ));
                        }
                    }
                    // 手动施加伤害
                    isCustomDamage.set(true);
                    entity.damage(source, damage);
                    return false; // 阻止原伤害
                }
            }
            return true;
        });
    }

    // 递归标志
    private static final ThreadLocal<Boolean> isCustomDamage = ThreadLocal.withInitial(() -> false);

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

    private static void applyPassiveEffects(ServerPlayerEntity player, float healthPercent) {
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.REGENERATION);

        if (healthPercent > 0.5f) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, -1, 4, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, -1, 4, false, false));
        } else {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, -1, 4, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, -1, 4, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 4, false, false));
        }
    }

    private static void removeAllTlipocaEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.REGENERATION);
    }
}