package doctor_m.mixin.ait_oxygenatedmixin;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.config.ConfigManager;
import doctor_m.module.space_plus.system.SpaceOxygenManager;
import doctor_m.util.SpaceEnvironmentUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
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

        if (living.getEquippedStack(EquipmentSlot.CHEST) != stack) {
            ci.cancel();
            return;
        }

        if (living instanceof PlayerEntity player && player.isCreative()) {
            ci.cancel();
            return;
        }

        var config = ConfigManager.getConfig();
        double consumeUnderwater = config.spacesuitOxygenConsumeUnderwater;
        double consumeSpace = config.spacesuitOxygenConsumeSpace;
        boolean envHasOxygen = SpaceEnvironmentUtil.hasEnvironmentalOxygen(living);
        boolean isSubmerged = living.isSubmergedInWater();

        // ====== 水下分支 ======
        if (isSubmerged) {
            double oxygen = SpaceOxygenManager.getOxygen(stack);
            if (oxygen > 0) {
                living.setAir(300);
                living.removeStatusEffect(StatusEffects.WITHER);
                if (world.getTime() % 40 == 0) {
                    SpaceOxygenManager.consumeOxygen(stack, consumeUnderwater);
                }
                ci.cancel();
                return;
            }

            // 宇航服没氧 → 看环境
            if (envHasOxygen) {
                // 地球水下：走原版憋气，不干预
                return;
            } else {
                // 太空水下：窒息
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, false));
                ci.cancel();
                return;
            }
        }

        // ====== 非水下环境 ======
        if (envHasOxygen) {
            // 环境本身有氧：不需要宇航服供氧，移除 wither
            living.removeStatusEffect(StatusEffects.WITHER);
            ci.cancel();
            return;
        }

        // ====== 无氧环境（太空等）======
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