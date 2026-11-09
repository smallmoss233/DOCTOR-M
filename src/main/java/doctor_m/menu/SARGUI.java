package doctor_m.menu;

import doctor_m.recipe.AssemblyRecipe;
import doctor_m.recipe.ModRecipes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

import java.util.List;

public class SARGUI extends ScreenHandler {

    public SARGUI(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(1));
    }

    public SARGUI(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModMenus.SAR_WORKBENCH_MENU, syncId);
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (player.getWorld().isClient) return false;

        List<AssemblyRecipe> recipes = player.getWorld().getRecipeManager().listAllOfType(ModRecipes.ASSEMBLY_TYPE);

        if (id >= 0 && id < recipes.size()) {
            AssemblyRecipe recipe = recipes.get(id);
            if (recipe.canCraft(player)) {
                recipe.craft(player);
                return true;
            }
        }
        return false;
    }
}