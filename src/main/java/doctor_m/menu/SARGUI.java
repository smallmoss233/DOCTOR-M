package doctor_m.menu;

import doctor_m.recipe.AssemblyRecipe;
import doctor_m.recipe.ModRecipes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.List;

public class SARGUI extends ScreenHandler {

    public SARGUI(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(1));
    }

    public SARGUI(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModMenus.SAR_WORKBENCH_MENU, syncId);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, -9999, -9999));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, -9999, -9999));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (player.getWorld().isClient) return false;

        List<AssemblyRecipe> recipes = player.getWorld().getRecipeManager().listAllOfType(ModRecipes.ASSEMBLY_TYPE);

        for (AssemblyRecipe recipe : recipes) {
            if (recipe.getId().toString().hashCode() == id) {
                if (recipe.canCraft(player)) {
                    recipe.craft(player);
                    return true;
                }
                break;
            }
        }
        return false;
    }
}