package com.example.doctor_m.mixin.ait_oxygenatedmixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry;
import doctor_m.module.ait_space_mixin.SpaceOxygenManager;
import doctor_m.util.config.ConfigManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpacesuitItem.class)
public abstract class MixinSpacesuitItem {

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void onInventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        SpacesuitItem self = (SpacesuitItem)(Object)this;

        if (self.getType() != ArmorItem.Type.CHESTPLATE) return;
        if (world.isClient()) return;
        if (!(entity instanceof LivingEntity living)) return;

        if (living.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST) != stack) {
            ci.cancel();
            return;
        }

        if (living instanceof PlayerEntity player && player.isCreative()) {
            ci.cancel();
            return;
        }

        // ====== 检测环境是否有氧 ======
        boolean worldHasOxygen = true;
        try {
            var planet = PlanetRegistry.getInstance().get(world);
            if (planet != null) {
                worldHasOxygen = planet.hasOxygen();
            }
        } catch (Exception ignored) {}

        boolean isTardis = world.getRegistryKey().getValue().getNamespace().equals("ait") &&
                world.getRegistryKey().getValue().getPath().startsWith("tardis");
        boolean hasOxygenated = living.hasStatusEffect(AITStatusEffects.OXYGENATED);

        boolean isSubmerged = living.isSubmergedInWater();
        boolean isHeadInsideBlock = !world.getBlockState(living.getBlockPos().up(1)).isAir();

        // ====== 读取配置 ======
        var config = ConfigManager.getConfig();
        double consumeUnderwater = config.spacesuitOxygenConsumeUnderwater;
        double consumeSpace = config.spacesuitOxygenConsumeSpace;

        // ====== 核心逻辑：水下呼吸 ======
        if (isSubmerged) {
            double oxygen = SpaceOxygenManager.getOxygen(stack);
            if (oxygen > 0) {
                living.setAir(300);
                living.removeStatusEffect(StatusEffects.WITHER);
                // 使用配置的消耗量（每2秒消耗）
                if (world.getTime() % 40 == 0) {
                    SpaceOxygenManager.consumeOxygen(stack, consumeUnderwater);
                }
            } else {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, false));
            }
            ci.cancel();
            return;
        }

        // ====== 非水下环境 ======
        boolean isUnbreathableEnvironment = isHeadInsideBlock;
        boolean hasOxygen = (worldHasOxygen || isTardis || hasOxygenated) && !isUnbreathableEnvironment;

        if (hasOxygen) {
            living.removeStatusEffect(StatusEffects.WITHER);
            ci.cancel();
            return;
        }

        // 无氧环境（太空等）
        if (world.getTime() % 60 == 0) {
            double current = SpaceOxygenManager.getOxygen(stack);
            if (current > 0) {
                SpaceOxygenManager.consumeOxygen(stack, consumeSpace);
            }
        }

        if (world.getTime() % 40 == 0) {
            double oxygen = SpaceOxygenManager.getOxygen(stack);
            if (oxygen > 0) {
                living.removeStatusEffect(StatusEffects.WITHER);
            } else {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, false));
            }
        }

        ci.cancel();
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        SpacesuitItem self = (SpacesuitItem)(Object)this;
        if (self.getType() != ArmorItem.Type.CHESTPLATE) return;

        double oxygen = SpaceOxygenManager.getOxygen(stack);
        double maxOxygen = SpaceOxygenManager.MAX_OXYGEN;
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, maxOxygen));

        ci.cancel();
    }
}