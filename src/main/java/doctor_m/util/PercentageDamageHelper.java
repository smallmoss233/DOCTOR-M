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

    public static class Config {
        public final long cooldownTicks;
        public final double damageFactor;
        public final double minPercent;
        public final Predicate<PlayerEntity> enableCondition;

        public Config(long cooldownTicks, double damageFactor, double minPercent, Predicate<PlayerEntity> enableCondition) {
            this.cooldownTicks = cooldownTicks;
            this.damageFactor = damageFactor;
            this.minPercent = minPercent;
            this.enableCondition = enableCondition;
        }
    }

    private final ConcurrentHashMap<UUID, Long> lastExtraDamageTick = new ConcurrentHashMap<>();

    public PercentageDamageHelper(Config config) {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(source.getSource() instanceof PlayerEntity)) return true;
            PlayerEntity player = (PlayerEntity) source.getSource();
            if (!config.enableCondition.test(player)) return true;
            if (!(entity instanceof LivingEntity)) return true;
            LivingEntity target = (LivingEntity) entity;
            if (target.isDead() || target.getHealth() <= 0) return true;

            UUID targetId = target.getUuid();
            long now = target.getWorld().getTime();
            Long last = lastExtraDamageTick.get(targetId);
            if (last != null && now - last < config.cooldownTicks) {
                return true;
            }
            lastExtraDamageTick.put(targetId, now);

            double percent = getDamagePercent(player, source);
            percent = Math.max(percent, config.minPercent);
            percent *= config.damageFactor;
            float percentDamage = target.getMaxHealth() * (float) (percent / 100.0);
            if (percentDamage <= 0) return true;

            float newHealth = target.getHealth() - percentDamage;
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
                || name.equals("freeze") || name.equals("dragonBreath")
                || name.equals("arrow"));
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