package doctor_m.mixin.doctor_m;

import dev.amble.ait.core.item.SonicItem;
import doctor_m.Item.items;
import doctor_m.block.ModBlocks;
import doctor_m.module.sonic_plus.UpgradeModuleManager;
import mosslib.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void doctor_m$regenerateFuel(ItemStack stack, World world, Entity entity,
                                         int slot, boolean selected, CallbackInfo ci) {
        if (world.isClient() || !(stack.getItem() instanceof SonicItem sonic)) return;
        if (!UpgradeModuleManager.hasRegenerationModule(stack)) return;
        if (world.getTime() % 100 != 0) return;

        if (sonic.getCurrentFuel(stack) < sonic.getMaxFuel(stack)) {
            sonic.addFuel(1.0, stack);
        }
    }

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void doctor_m$moduleTooltips(ItemStack stack, @Nullable World world,
                                         List<Text> tooltip, TooltipContext context,
                                         CallbackInfo ci) {
        Item item = stack.getItem();

        if (item == items.ENERGY_UPGRADE_MODULE || item == items.REGENERATION_MODULE) {
            tooltip.add(t("message.tooltip.doctor_m.upgrade"));
            tooltip.add(t("message.tooltip.doctor_m.removed_upgrade"));
            tooltip.add(t(item == items.ENERGY_UPGRADE_MODULE
                    ? "message.tooltip.doctor_m.energy_upgrade.desc"
                    : "message.tooltip.doctor_m.regeneration_module.desc"));
            return;
        }

        if (item == ModBlocks.COFFEE_MACHINE.asItem()) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.tooltip.doctor_m.coffee_machine"));
        }
        else if (item == ModBlocks.DOLL_JIN_MARY.asItem()) {
            tooltip.add(t("message.tooltip.doctor_m.doll_jin_mary"));
        }
        else if (item == ModBlocks.DOLL_SMALLMOSS_OLD.asItem()) {
            tooltip.add(t("message.tooltip.doctor_m.doll_smallmoss_old"));
        }
        else if (item == ModBlocks.DOLL_SIGEERTE.asItem()) {
            tooltip.add(t("message.tooltip.doctor_m.doll_sigeerte"));
        }
        else if (item == ModBlocks.DOLL_TSINAFS_BCIM.asItem()) {
            tooltip.add(t("message.tooltip.doctor_m.doll_tsinafs_bcim"));
        }
        else if (item == ModBlocks.DOLL_TC020.asItem()) {
            tooltip.add(t("message.tooltip.doctor_m.doll_tc020"));
        }
        else if (item == ModBlocks.DOLL_ASDJDFK.asItem()) {
            tooltip.add(t("message.tooltip.doctor_m.doll_asdjdfk"));
        }
        else if (item == ModBlocks.DOLL_TIANX.asItem()) {
            tooltip.add(t("message.tooltip.doctor_m.doll_tianx"));
        }
        else if (item == ModBlocks.OXYGEN_CHARGER.asItem()) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.doctor_m.oxygen_charger"));
        }
        else if (item == ModBlocks.UNDERWATER_OXYGEN_GENERATOR.asItem()) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.doctor_m.underwater_oxygen_generator"));
        }
        else if (item == ModBlocks.EYE_OF_HARMONY_OBELISK.asItem()) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.tooltip.doctor_m.eye_of_harmony_obelisk"));
        }
        else if (item == ModBlocks.TOYOTA_SPINNING_ROTOR.asItem()) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.tooltip.doctor_m.toyota_spinning_rotor"));
        }
        else if (stack.isOf(items.SEAL_OF_THE_HIGH_COUNCIL)) {
            tooltip.add(t("message.doctor_m.tip.not.done"));
        }
    }

    private static Text t(String key) {
        return Text.translatable(key).formatted(Formatting.GRAY, Formatting.ITALIC);
    }
}