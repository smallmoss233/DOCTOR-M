package doctor_m.module.space_plus.system;

import dev.amble.ait.core.AITStatusEffects;
import doctor_m.config.ConfigManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class OxygenSystem {
    public static final String OXYGEN_KEY = "oxygen";

    public static double getMaxOxygen() {
        return ConfigManager.getConfig().spacesuitMaxOxygen;
    }

    public static double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    public static void setOxygen(ItemStack stack, double amount) {
        double clamped = Math.max(0.0, Math.min(amount, getMaxOxygen()));
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, clamped);
    }

    public static void consumeOxygen(ItemStack stack, double amount) {
        double current = getOxygen(stack);
        setOxygen(stack, current - amount);
    }

    public static void refillOxygen(ItemStack stack, double amount) {
        double current = getOxygen(stack);
        setOxygen(stack, current + amount);
    }

    public static void updateOxygenatedStatus(LivingEntity entity, ItemStack chestStack) {
        double oxygen = getOxygen(chestStack);
        if (oxygen > 0) {
            entity.addStatusEffect(new StatusEffectInstance(AITStatusEffects.OXYGENATED, 40, 0, true, false));
        } else {
            entity.removeStatusEffect(AITStatusEffects.OXYGENATED);
        }
    }
}