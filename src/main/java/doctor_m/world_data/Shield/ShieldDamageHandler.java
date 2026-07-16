package doctor_m.world_data.Shield;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_itme.ShieldCoreItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ShieldDamageHandler {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;

            ItemStack shield = findShield(player);
            if (shield.isEmpty()) return true;

            int energy = ShieldCoreItem.getEnergy(shield);
            int cost = (int) (amount * ShieldCoreItem.getCostPerDamage());
            if (energy < cost) return true;

            if (!ShieldCoreItem.consumeEnergy(shield, cost)) return true;

            // 发送网络包到客户端
            ShieldNetworking.sendShieldActivation(player);

            // 计算实际伤害（0.01%）
            float actualDamage = amount * 0.0001f;
            if (actualDamage > 0) {
                float newHealth = player.getHealth() - actualDamage;
                player.setHealth(Math.max(newHealth, 0));
            }
            return false; // 取消原伤害
        });
    }

    private static ItemStack findShield(PlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) return ItemStack.EMPTY;

        // 修复：getInventory() 返回 Map<String, Map<String, TrinketInventory>>
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