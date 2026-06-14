package doctor_m.util;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.function.Predicate;

public class PercentageDamageHelper {

    // 冷却时间映射 (目标UUID -> 上次附加伤害的时间)
    private static final ConcurrentHashMap<UUID, Long> lastExtraDamageTick = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 20; // 1秒 = 20 ticks

    private static Predicate<PlayerEntity> enablePredicate = player -> false;

    public static void register(Predicate<PlayerEntity> condition) {
        enablePredicate = condition;
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // 仅处理玩家攻击
            if (!(source.getSource() instanceof PlayerEntity)) return true;
            PlayerEntity player = (PlayerEntity) source.getSource();
            if (!enablePredicate.test(player)) return true;
            if (!(entity instanceof LivingEntity)) return true;
            LivingEntity target = (LivingEntity) entity;
            if (target.isDead() || target.getHealth() <= 0) return true;

            UUID targetId = target.getUuid();
            long now = target.getWorld().getTime();

            // 检查冷却
            Long last = lastExtraDamageTick.get(targetId);
            if (last != null && now - last < COOLDOWN_TICKS) {
                // 冷却中，不附加额外伤害，仅原版伤害生效
                return true;
            }

            // 更新冷却时间
            lastExtraDamageTick.put(targetId, now);

            // 计算百分比伤害（基于目标最大生命值）
            double percent = getDamagePercent(player, source);
            float percentDamage = target.getMaxHealth() * (float) (percent / 100.0);
            if (percentDamage <= 0) return true;

            // 直接扣除百分比伤害（不包含原始伤害）
            float newHealth = target.getHealth() - percentDamage;
            if (newHealth <= 0) {
                target.setHealth(0);
                target.onDeath(source);
            } else {
                target.setHealth(newHealth);
            }

            // 原版伤害依然生效（返回 true）
            return true;
        });
    }

    private static double getDamagePercent(PlayerEntity player, DamageSource source) {
        String name = source.getName();
        boolean isPhysical = !(name.equals("magic") || name.equals("indirectMagic")
                || name.equals("wither") || name.equals("thorns")
                || name.equals("sonic_boom") || name.equals("lava")
                || name.equals("inFire") || name.equals("onFire")
                || name.equals("drown") || name.equals("starve")
                || name.equals("freeze") || name.equals("dragonBreath"));
        if (isPhysical) {
            double attack = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).getValue();
            return Math.max(attack, 1);
        } else {
            return 5.0;
        }
    }

    public static Predicate<PlayerEntity> hasAnyOfItems(Item... items) {
        return player -> TrinketsApi.getTrinketComponent(player)
                .map(comp -> comp.isEquipped(stack -> {
                    for (Item item : items) {
                        if (stack.getItem() == item) return true;
                    }
                    return false;
                }))
                .orElse(false);
    }
}