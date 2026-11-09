package doctor_m.recipe;

import doctor_m.DOCTORM;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModRecipes {

    public static final RecipeType<AssemblyRecipe> ASSEMBLY_TYPE =
            Registry.register(Registries.RECIPE_TYPE, DOCTORM.id("assembly"), new RecipeType<AssemblyRecipe>() {
                @Override
                public String toString() { return "assembly"; }
            });

    public static final RecipeSerializer<AssemblyRecipe> ASSEMBLY_SERIALIZER =
            Registry.register(Registries.RECIPE_SERIALIZER, DOCTORM.id("assembly"), new AssemblyRecipeSerializer());

    public static void register() {
    }
}