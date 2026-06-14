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

    private static final ConcurrentHashMap<UUID, Long> lastApplyTime = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 5;
    private static Predicate<PlayerEntity> enablePredicate = player -> false;

    public static void register(Predicate<PlayerEntity> condition) {
        enablePredicate = condition;
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(source.getSource() instanceof PlayerEntity)) return true;
            PlayerEntity player = (PlayerEntity) source.getSource();
            if (!enablePredicate.test(player)) return true;
            if (!(entity instanceof LivingEntity)) return true;
            LivingEntity target = (LivingEntity) entity;
            if (target.isDead() || target.getHealth() <= 0) return true;

            UUID targetId = target.getUuid();
            long now = target.getWorld().getTime();
            Long lastTime = lastApplyTime.get(targetId);
            if (lastTime != null && now - lastTime < COOLDOWN_TICKS) {
                return false;
            }
            lastApplyTime.put(targetId, now);

            double percent = getDamagePercent(player, source);
            float percentDamage = target.getMaxHealth() * (float) (percent / 100.0);
            float originalDamage = amount;

            float totalDamage = percentDamage + originalDamage;
            if (totalDamage <= 0) return true;

            float newHealth = target.getHealth() - totalDamage;
            if (newHealth <= 0) {
                target.setHealth(0);
                target.onDeath(source);
            } else {
                target.setHealth(newHealth);
            }
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