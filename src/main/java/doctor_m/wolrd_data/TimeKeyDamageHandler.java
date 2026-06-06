package doctor_m.wolrd_data;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import doctor_m.Item.data_itme.time_key;

import java.util.List;

public class TimeKeyDamageHandler {

    private static final ThreadLocal<Boolean> isCustomDamage = ThreadLocal.withInitial(() -> false);

    public static void register() {
        // 伤害限制
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (isCustomDamage.get()) {
                isCustomDamage.set(false);
                return true;
            }

            if (entity instanceof PlayerEntity player) {
                boolean hasTimeKey = TrinketsApi.getTrinketComponent(player)
                        .map(component -> {
                            List<Pair<SlotReference, ItemStack>> equipped = component.getEquipped(stack -> stack.getItem() instanceof time_key);
                            return !equipped.isEmpty();
                        })
                        .orElse(false);

                if (hasTimeKey) {
                    float maxHealth = player.getMaxHealth();
                    float maxAllowed = maxHealth * 0.2f;
                    float newAmount = Math.min(amount, maxAllowed);

                    if (newAmount != amount) {
                        isCustomDamage.set(true);
                        player.damage(source, newAmount);
                        return false;
                    }
                }
            }
            return true;
        });

        // 生命恢复 II 效果（每 tick 检查）
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // 重生后重新检查效果
            updateRegenerationEffect(newPlayer);
        });
        // 由于玩家登录、切换维度等也需要，用 tick 事件更稳妥
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
                updateRegenerationEffect(player);
            }
        });
    }

    private static void updateRegenerationEffect(PlayerEntity player) {
        boolean hasTimeKey = TrinketsApi.getTrinketComponent(player)
                .map(component -> component.isEquipped(stack -> stack.getItem() instanceof time_key))
                .orElse(false);
        boolean hasEffect = player.hasStatusEffect(StatusEffects.REGENERATION);
        if (hasTimeKey) {
            if (!hasEffect || player.getStatusEffect(StatusEffects.REGENERATION).getAmplifier() != 1) {
                // 添加生命恢复 II（放大器 1 表示 II 级，因为 0 = I）
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, Integer.MAX_VALUE, 1, true, false));
            }
        } else {
            if (hasEffect) {
                player.removeStatusEffect(StatusEffects.REGENERATION);
            }
        }
    }
}