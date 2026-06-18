package com.example.doctor_m.mixin;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.module.space.SpaceOxygenManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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

        // 1. 只处理胸甲
        if (self.getType() != ArmorItem.Type.CHESTPLATE) return;
        if (world.isClient()) return;
        if (!(entity instanceof LivingEntity living)) return;

        // 2. 创造模式玩家不消耗氧气
        if (entity instanceof PlayerEntity player && player.isCreative()) {
            ci.cancel();
            return;
        }

        // 3. 只有穿在胸甲槽位时才消耗氧气
        if (!(entity instanceof PlayerEntity player)) return;
        if (player.getInventory().armor.get(2) != stack) {
            ci.cancel();
            return;
        }

        // ====== 关键改动：如果玩家有 OXYGENATED 效果（氧气机覆盖），不消耗氧气 ======
        if (player.hasStatusEffect(AITStatusEffects.OXYGENATED)) {
            ci.cancel();
            return;
        }

        // 4. 每秒消耗氧气（每60 tick，即3秒消耗1点，保持你的设定）
        if (world.getTime() % 60 == 0) {
            double current = SpaceOxygenManager.getOxygen(stack);
            if (current > 0) {
                SpaceOxygenManager.consumeOxygen(stack, 1.0);
            }
        }

        // 5. 每2秒更新缺氧状态
        if (world.getTime() % 40 == 0) {
            SpaceOxygenManager.updatePlayerOxygenStatus(living, stack);
        }

        ci.cancel();
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        SpacesuitItem self = (SpacesuitItem)(Object)this;
        if (self.getType() != ArmorItem.Type.CHESTPLATE) return;

        double oxygen = SpaceOxygenManager.getOxygen(stack);
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, SpaceOxygenManager.MAX_OXYGEN));

        // 取消 AIT 原有的 Tooltip 显示
        ci.cancel();
    }
}