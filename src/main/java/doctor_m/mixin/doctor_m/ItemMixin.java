package doctor_m.mixin.doctor_m;

import doctor_m.module.sonic_plus.UpgradeModuleManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.item.SonicItem;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {

    //能量再生核心恢复能量
    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void doctor_m$regenerateFuel(ItemStack stack, World world, Entity entity,
                                         int slot, boolean selected, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!(stack.getItem() instanceof SonicItem sonic)) return;
        if (!UpgradeModuleManager.hasRegenerationModule(stack)) return;

        // 每 5 秒（100 ticks）恢复 1 点能量
        if (world.getTime() % 100 != 0) return;

        double current = sonic.getCurrentFuel(stack);
        double max = sonic.getMaxFuel(stack);

        if (current < max) {
            sonic.addFuel(1.0, stack);
        }
    }
    //模块提示
    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void doctor_m$moduleTooltips(ItemStack stack, @Nullable World world,
                                         List<Text> tooltip, TooltipContext context,
                                         CallbackInfo ci) {
        Identifier id = Registries.ITEM.getId(stack.getItem());

        if (id.equals(UpgradeModuleManager.ENERGY_UPGRADE)) {
            tooltip.add(Text.translatable("message.tooltip.doctor_m.upgrade")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("message.tooltip.doctor_m.removed_upgrade")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("message.tooltip.doctor_m.energy_upgrade.desc")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        } else if (id.equals(UpgradeModuleManager.REGENERATION_MODULE)) {
            tooltip.add(Text.translatable("message.tooltip.doctor_m.upgrade")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("message.tooltip.doctor_m.removed_upgrade")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("message.tooltip.doctor_m.regeneration_module.desc")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }
    }
}