package com.example.doctor_m.mixin;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import dev.amble.ait.module.planet.core.space.planet.Planet;
import doctor_m.module.ait_space_mixin.SpaceOxygenManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Planet.class)
public class MixinPlanetOxygenOverride {

    @Inject(method = "hasOxygenInTank", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onHasOxygenInTank(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof SpacesuitItem) {
            double oxygen = SpaceOxygenManager.getOxygen(chest);
            cir.setReturnValue(oxygen > 0);
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hasFullSuit", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onHasFullSuit(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack head = entity.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack legs = entity.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack feet = entity.getEquippedStack(EquipmentSlot.FEET);
        boolean fullSuit = chest.getItem() instanceof SpacesuitItem &&
                head.getItem() instanceof SpacesuitItem &&
                legs.getItem() instanceof SpacesuitItem &&
                feet.getItem() instanceof SpacesuitItem;
        cir.setReturnValue(fullSuit);
    }
}