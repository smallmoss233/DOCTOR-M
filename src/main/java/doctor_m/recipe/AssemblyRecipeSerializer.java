package doctor_m.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AssemblyRecipeSerializer implements RecipeSerializer<AssemblyRecipe> {

    @Override
    public AssemblyRecipe read(Identifier id, JsonObject json) {
        JsonArray inputsArray = json.getAsJsonArray("inputs");
        List<Ingredient> inputs = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();

        for (JsonElement element : inputsArray) {
            inputs.add(Ingredient.fromJson(element));
            JsonObject obj = element.getAsJsonObject();
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            inputCounts.add(count);
        }

        JsonObject outputObj = json.getAsJsonObject("output");
        Identifier outputId = new Identifier(outputObj.get("item").getAsString());
        int count = outputObj.has("count") ? outputObj.get("count").getAsInt() : 1;
        ItemStack output = new ItemStack(Registries.ITEM.get(outputId), count);

        return new AssemblyRecipe(id, inputs, inputCounts, output);
    }

    @Override
    public AssemblyRecipe read(Identifier id, PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<Ingredient> inputs = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            inputs.add(Ingredient.fromPacket(buf));
            inputCounts.add(buf.readVarInt());
        }

        ItemStack output = buf.readItemStack();
        return new AssemblyRecipe(id, inputs, inputCounts, output);
    }

    @Override
    public void write(PacketByteBuf buf, AssemblyRecipe recipe) {
        buf.writeVarInt(recipe.getInputs().size());

        for (int i = 0; i < recipe.getInputs().size(); i++) {
            Ingredient ingredient = recipe.getInputs().get(i);
            ingredient.write(buf);
            buf.writeVarInt(recipe.getInputCounts().get(i));
        }

        buf.writeItemStack(recipe.getOutput());
    }
}