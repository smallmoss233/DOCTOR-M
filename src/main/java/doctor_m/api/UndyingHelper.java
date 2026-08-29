package doctor_m.api;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_item.KeytoTimeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class UndyingHelper {

    public static boolean hasKTT(PlayerEntity player) {
        if (player == null) return false;

        if (player.getInventory() == null) return false;

        if (player.getMainHandStack().getItem() instanceof KeytoTimeItem) return true;

        if (player.getOffHandStack().getItem() instanceof KeytoTimeItem) return true;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof KeytoTimeItem) return true;
        }

        var optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isPresent()) {
            var component = optional.get();
            for (var equipped : component.getAllEquipped()) {
                if (equipped.getRight().getItem() instanceof KeytoTimeItem) return true;
            }
        }

        return false;
    }
}