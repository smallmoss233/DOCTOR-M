package doctor_m.module.space_plus.system;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class SpaceOxygenManager {
    public static final String OXYGEN_KEY = "doctor_m_oxygen";
    public static final double MAX_OXYGEN = 1200.0;

    public static double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    public static void setOxygen(ItemStack stack, double amount) {
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, MAX_OXYGEN));
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