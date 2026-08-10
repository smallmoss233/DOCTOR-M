package doctor_m.module.space_plus;

import doctor_m.config.ConfigManager;
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
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, getMaxOxygen()));
    }

    public static void consumeOxygen(ItemStack stack, double amount) {
        double current = getOxygen(stack);
        setOxygen(stack, current - amount);
    }

    public static void refillOxygen(ItemStack stack, double amount) {
        double current = getOxygen(stack);
        setOxygen(stack, current + amount);
    }
}