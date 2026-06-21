package doctor_m.module.creativity.creativity_data;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class TlipocaMaterial implements ToolMaterial {
    public static final ToolMaterial INSTANCE = new TlipocaMaterial();

    @Override
    public int getDurability() {
        return 9999;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 0.0f;
    }

    @Override
    public float getAttackDamage() {
        return 20.0f;
    }

    @Override
    public int getMiningLevel() {
        return 0;
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}