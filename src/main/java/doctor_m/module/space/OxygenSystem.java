package doctor_m.module.space;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import dev.amble.ait.core.AITStatusEffects;

public class OxygenSystem {
    public static final String OXYGEN_KEY = "oxygen";
    public static final double MAX_OXYGEN = 1200.0; // 20 分钟

    // 获取氧气值（若不存在则返回 0）
    public static double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    // 设置氧气值（写入 NBT）
    public static void setOxygen(ItemStack stack, double amount) {
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, MAX_OXYGEN));
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
            // 有氧气：添加或刷新标记（持续 2 秒）
            entity.addStatusEffect(new StatusEffectInstance(AITStatusEffects.OXYGENATED, 40, 0, true, false));
        } else {
            // 无氧气：移除标记
            entity.removeStatusEffect(AITStatusEffects.OXYGENATED);
        }
    }
}