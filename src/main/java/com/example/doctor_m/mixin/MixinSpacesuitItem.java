package com.example.doctor_m.mixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry;
import doctor_m.module.ait_space_mixin.SpaceOxygenManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
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
        if (!(entity instanceof PlayerEntity player)) return;
        if (player.getInventory().armor.get(2) != stack) {
            ci.cancel();
            return;
        }
        if (player.isCreative()) {
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
        boolean hasOxygenated = player.hasStatusEffect(AITStatusEffects.OXYGENATED);

        // 水下检测（即使世界有氧，也视为无氧环境）
        boolean isSubmerged = player.isSubmergedInWater();
        boolean isHeadInsideBlock = !world.getBlockState(player.getBlockPos().up(1)).isAir();

        // ====== 核心逻辑：水下呼吸 ======
        if (isSubmerged) {
            // 水下：无论世界是否有氧，都消耗氧气提供呼吸
            double oxygen = SpaceOxygenManager.getOxygen(stack);
            if (oxygen > 0) {
                // 有氧气：重置呼吸条，移除溺水效果
                player.setAir(300); // 重置呼吸条到满
                player.removeStatusEffect(StatusEffects.WITHER);
                // 消耗氧气（每2秒消耗1点，比普通无氧环境更节能？可根据需要调整）
                if (world.getTime() % 40 == 0) {
                    SpaceOxygenManager.consumeOxygen(stack, 0.5);
                }
            } else {
                // 无氧气：允许溺水，添加缺氧效果
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, false));
                // 不干预呼吸条，让原版溺水机制工作
            }
            ci.cancel();
            return;
        }

        // ====== 非水下环境 ======
        boolean isUnbreathableEnvironment = isHeadInsideBlock;
        boolean hasOxygen = (worldHasOxygen || isTardis || hasOxygenated) && !isUnbreathableEnvironment;

        if (hasOxygen) {
            player.removeStatusEffect(StatusEffects.WITHER);
            ci.cancel();
            return;
        }

        // 无氧环境（太空等）
        if (world.getTime() % 60 == 0) {
            double current = SpaceOxygenManager.getOxygen(stack);
            if (current > 0) {
                SpaceOxygenManager.consumeOxygen(stack, 1.0);
            }
        }

        if (world.getTime() % 40 == 0) {
            double oxygen = SpaceOxygenManager.getOxygen(stack);
            if (oxygen > 0) {
                player.removeStatusEffect(StatusEffects.WITHER);
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, false));
            }
        }

        ci.cancel();
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        SpacesuitItem self = (SpacesuitItem)(Object)this;
        if (self.getType() != ArmorItem.Type.CHESTPLATE) return;

        double oxygen = SpaceOxygenManager.getOxygen(stack);
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, SpaceOxygenManager.MAX_OXYGEN));

        ci.cancel();
    }
}