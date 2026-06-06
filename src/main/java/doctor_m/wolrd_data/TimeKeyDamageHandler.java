package doctor_m.wolrd_data;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import doctor_m.Item.data_itme.time_key;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimeKeyDamageHandler {

    private static final ThreadLocal<Boolean> isCustomDamage = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Long> revivalCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 200; // 10秒 = 200 ticks
    private static final long FIX_INTERVAL_TICKS = 200; // 每 10 秒修复一次效果

    public static void register() {
        // 1. 伤害限制 + 复活处理
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (isCustomDamage.get()) {
                isCustomDamage.set(false);
                return true;
            }

            if (entity instanceof PlayerEntity player) {
                boolean hasTimeKey = isTimeKeyEquipped(player);
                if (hasTimeKey) {
                    // 伤害限制
                    float maxHealth = player.getMaxHealth();
                    float maxAllowed = maxHealth * 0.2f;
                    float newAmount = Math.min(amount, maxAllowed);
                    float newHealth = player.getHealth() - newAmount;

                    // 致命伤害且未冷却 -> 复活
                    if (newHealth <= 0 && !isInCooldown(player)) {
                        revivePlayer((ServerPlayerEntity) player);
                        long currentTime = player.getEntityWorld().getTime();
                        revivalCooldown.put(player.getUuid(), currentTime + COOLDOWN_TICKS);
                        return false;
                    }

                    // 非致命伤害，应用限制
                    if (newAmount != amount) {
                        isCustomDamage.set(true);
                        player.damage(source, newAmount);
                        return false;
                    }
                }
            }
            return true;
        });
    }

    private static boolean isTimeKeyEquipped(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.isEquipped(stack -> stack.getItem() instanceof time_key))
                .orElse(false);
    }

    private static boolean isInCooldown(PlayerEntity player) {
        Long cooldownUntil = revivalCooldown.get(player.getUuid());
        if (cooldownUntil == null) return false;
        return player.getEntityWorld().getTime() < cooldownUntil;
    }

    private static void revivePlayer(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth());
        player.clearStatusEffects();
        // 自定义粒子
        for (int i = 0; i < 50; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.0;
            double y = player.getY() + player.getRandom().nextDouble() * 2.0;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.0;
            player.getServerWorld().spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.1);
            player.getServerWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0.05);
        }
        player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
        player.sendMessage(Text.translatable("txt.doctor_m.time_key_resurrection"), true);
        // 重新应用时间钥匙的效果（因为 clearStatusEffects 会清除生命恢复）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false));
        if (!player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
    }
}