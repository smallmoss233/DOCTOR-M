package com.example.doctor_m.mixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.core.effects.OxygenatedEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(OxygenatedEffect.class)
public class MixinOxygenatedEffect {

    /**
     * @reason 判断玩家是否拥有 OXYGENATED 效果（由氧气机或宇航服提供）
     */
    @Overwrite
    public static boolean isOxygenated(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) return false;
        // 如果玩家拥有 OXYGENATED 效果（由氧气机施加），返回 true
        return player.hasStatusEffect(AITStatusEffects.OXYGENATED);
    }
}