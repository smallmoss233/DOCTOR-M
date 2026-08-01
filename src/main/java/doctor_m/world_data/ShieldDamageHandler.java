package doctor_m.world_data;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_itme.ForceFieldShieldItem;
import doctor_m.Item.data_itme.ShieldCoreItem;
import doctor_m.network.ShieldNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class ShieldDamageHandler {

    private static final Map<PlayerEntity, Integer> LAST_SHIELD_TICK = new WeakHashMap<>();
    private static final int SHIELD_FX_COOLDOWN = 4;

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            // 1. 无敌/创造模式，直接跳过
            if (player.isInvulnerableTo(source)) return true;

            if (player.isInvulnerableTo(source)) return true;

            // 2. 原版盾牌优先（举着原版盾且伤害可被格挡）
            if (player.isBlocking()
                    && player.getActiveItem().getItem() instanceof ShieldItem
                    && !source.isIn(DamageTypeTags.BYPASSES_SHIELD)) {
                return true;
            }

            if (player.isUsingItem()
                    && player.getActiveItem().getItem() instanceof ForceFieldShieldItem) {
                return true; // 让力场盾牌自己处理，饰品栏护盾生成器不抢
            }

            // 4. 查找饰品栏护盾生成器
            ItemStack shield = findShield(player);
            if (shield.isEmpty()) return true;

            // 5. 计算消耗
            int cost = (int) (amount * ShieldCoreItem.getCostPerDamage());
            int energy = ShieldCoreItem.getEnergy(shield);
            if (energy < cost) return true; // 能量不足，击穿

            // 6. 扣能量
            if (!ShieldCoreItem.consumeEnergy(shield, cost)) return true;

            // 7. 0.01% 穿透伤害
            float actualDamage = amount * 0.0001f;
            if (actualDamage > 0) {
                float newHealth = player.getHealth() - actualDamage;
                player.setHealth(Math.max(newHealth, 0.01f));
            }

            // 8. 特效冷却发包
            int currentTick = player.server.getTicks();
            Integer lastTick = LAST_SHIELD_TICK.get(player);
            if (lastTick == null || currentTick - lastTick >= SHIELD_FX_COOLDOWN) {
                LAST_SHIELD_TICK.put(player, currentTick);
                ShieldNetworking.sendShieldActivation(player);
            }

            return false;
        });
    }

    private static ItemStack findShield(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> {
                    for (var group : component.getInventory().values()) {
                        for (var slot : group.values()) {
                            for (int i = 0; i < slot.size(); i++) {
                                ItemStack stack = slot.getStack(i);
                                if (stack.getItem() instanceof ShieldCoreItem) {
                                    return stack;
                                }
                            }
                        }
                    }
                    return ItemStack.EMPTY;
                })
                .orElse(ItemStack.EMPTY);
    }
}