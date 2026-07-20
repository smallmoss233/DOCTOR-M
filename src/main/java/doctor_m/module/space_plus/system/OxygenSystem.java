package doctor_m.module.space_plus.system;

import doctor_m.config.ConfigManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import dev.amble.ait.core.AITStatusEffects;

public class OxygenSystem {
    public static final String OXYGEN_KEY = "oxygen";
    // 移除硬编码的 MAX_OXYGEN

    // 获取当前配置的最大氧气容量（宇航服）
    public static double getMaxOxygen() {
        return ConfigManager.getConfig().spacesuitMaxOxygen;
    }

    // 获取氧气值（若不存在则返回 0）
    public static double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    // 设置氧气值（写入 NBT），自动限制最大值
    public static void setOxygen(ItemStack stack, double amount) {
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, getMaxOxygen()));
    }

    // 消耗氧气
    public static void consumeOxygen(ItemStack stack, double amount) {
        double current = getOxygen(stack);
        setOxygen(stack, current - amount);
    }

    // 补充氧气
    public static void refillOxygen(ItemStack stack, double amount) {
        double current = getOxygen(stack);
        setOxygen(stack, current + amount);
    }

    // 检查并更新氧气标记
    public static void updateOxygenatedStatus(LivingEntity entity, ItemStack chestStack) {
        double oxygen = getOxygen(chestStack);
        if (oxygen > 0) {
            entity.addStatusEffect(new StatusEffectInstance(AITStatusEffects.OXYGENATED, 40, 0, true, false));
        } else {
            entity.removeStatusEffect(AITStatusEffects.OXYGENATED);
        }
    }
}