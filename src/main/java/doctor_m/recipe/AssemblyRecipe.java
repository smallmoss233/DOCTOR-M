package doctor_m.recipe;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.List;

public class AssemblyRecipe implements Recipe<Inventory> {
    private final Identifier id;
    private final List<Ingredient> inputs;
    private final ItemStack output;

    public AssemblyRecipe(Identifier id, List<Ingredient> inputs, ItemStack output) {
        this.id = id;
        this.inputs = inputs;
        this.output = output;
    }

    public List<Ingredient> getInputs() { return inputs; }
    public ItemStack getOutput() { return output; }

    public boolean canCraft(PlayerEntity player) {
        for (Ingredient ingredient : inputs) {
            if (!player.getInventory().contains(ingredient.getMatchingStacks()[0])) return false;
        }
        return true;
    }

    public void craft(PlayerEntity player) {
        if (!canCraft(player)) return;

        for (Ingredient ingredient : inputs) {
            int requiredCount = ingredient.getMatchingStacks()[0].getCount();
            for (int i = 0; i < player.getInventory().size() && requiredCount > 0; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (ingredient.test(stack)) {
                    int remove = Math.min(stack.getCount(), requiredCount);
                    stack.decrement(remove);
                    requiredCount -= remove;
                }
            }
        }

        player.dropItem(output.copy(), false);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    @Override public boolean matches(Inventory inventory, World world) { return false; }
    @Override public ItemStack craft(Inventory inventory, DynamicRegistryManager manager) { return ItemStack.EMPTY; }
    @Override public boolean fits(int width, int height) { return true; }
    @Override public ItemStack getOutput(DynamicRegistryManager manager) { return output; }
    @Override public Identifier getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ASSEMBLY_SERIALIZER; }
    @Override public RecipeType<?> getType() { return ModRecipes.ASSEMBLY_TYPE; }
}