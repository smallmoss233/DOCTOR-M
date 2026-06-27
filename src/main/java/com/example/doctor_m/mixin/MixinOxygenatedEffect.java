package com.example.doctor_m.mixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.core.effects.OxygenatedEffect;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.module.ait_space_mixin.SpaceOxygenManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(OxygenatedEffect.class)
public class MixinOxygenatedEffect {

    /**
     * @reason 扩展为所有生物：宇航服有氧则判为有氧，同时保留氧气机效果
     */
    @Overwrite
    public static boolean isOxygenated(LivingEntity entity) {
        if (entity == null) return false;
        // 1. 如果有 OXYGENATED 效果（由氧气机或其它方式施加），直接返回 true
        if (entity.hasStatusEffect(AITStatusEffects.OXYGENATED)) {
            return true;
        }
        // 2. 检查胸甲是否为宇航服且氧气 > 0
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof SpacesuitItem) {
            return SpaceOxygenManager.getOxygen(chest) > 0;
        }
        return false;
    }
}