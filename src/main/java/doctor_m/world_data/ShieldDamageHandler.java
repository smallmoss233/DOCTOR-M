package doctor_m.world_data;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_itme.ShieldCoreItem;
import doctor_m.network.ShieldNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShieldDamageHandler {
    // 护盾特效冷却：避免高频受伤导致高频发包
    private static final Map<UUID, Integer> LAST_SHIELD_TICK = new HashMap<>();
    private static final int SHIELD_FX_COOLDOWN = 4; // 0.2秒

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            ItemStack shield = findShield(player);
            if (shield.isEmpty()) return true;

            int energy = ShieldCoreItem.getEnergy(shield);
            int cost = (int) (amount * ShieldCoreItem.getCostPerDamage());
            if (energy < cost) return true;

            if (!ShieldCoreItem.consumeEnergy(shield, cost)) return true;

            // 扣除 0.01% 穿透伤害
            float actualDamage = amount * 0.0001f;
            if (actualDamage > 0) {
                float newHealth = player.getHealth() - actualDamage;
                player.setHealth(Math.max(newHealth, 0));
            }

            // 特效冷却：挡伤害逻辑每 tick 都走，但发包/声音有冷却
            int currentTick = player.getWorld().getServer().getTicks();
            Integer lastTick = LAST_SHIELD_TICK.get(player.getUuid());
            if (lastTick == null || currentTick - lastTick >= SHIELD_FX_COOLDOWN) {
                LAST_SHIELD_TICK.put(player.getUuid(), currentTick);
                ShieldNetworking.sendShieldActivation(player);
            }

            return false; // 取消原伤害
        });
    }

    private static ItemStack findShield(PlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) return ItemStack.EMPTY;

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
    }
}