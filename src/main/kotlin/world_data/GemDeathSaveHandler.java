package world_data;

import doctor_m.Item.data_itme.fragment.mystery_gem;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import dev.emi.trinkets.api.TrinketsApi;

public class GemDeathSaveHandler {
    public static void register() {
        ServerPlayerEvents.ALLOW_DEATH.register((ServerPlayerEntity player, DamageSource damageSource, float damageAmount) -> {
            // 返回 false = 阻止死亡
            final boolean[] saved = {false};

            TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                component.getEquipped(stack -> stack.getItem() instanceof mystery_gem).forEach(pair -> {
                    ItemStack gemStack = pair.getRight();
                    if (mystery_gem.tryTriggerDeathSave(player, gemStack)) {
                        saved[0] = true;
                        // 设置无敌帧防止立刻再死
                        player.setHealth(1.0f);
                        player.clearStatusEffects();
                        // 重新应用爆发效果（clearStatusEffects 清掉了）
                        mystery_gem.tryTriggerDeathSave(player, gemStack); // 重新触发一次确保效果上满
                        // 或者更好的做法：把 tryTriggerDeathSave 拆成 check 和 apply 两部分
                    }
                });
            });

            return !saved[0];
        });
    }
}