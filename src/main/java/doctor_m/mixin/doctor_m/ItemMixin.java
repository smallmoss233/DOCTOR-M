package doctor_m.mixin.doctor_m;

import dev.amble.ait.core.item.SonicItem;
import doctor_m.Item.items;
import doctor_m.module.sonic_plus.UpgradeModuleManager;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
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

        // 升级模块
        if (item == items.ENERGY_UPGRADE_MODULE || item == items.REGENERATION_MODULE) {
            tooltip.add(t("message.tooltip.doctor_m.upgrade"));
            tooltip.add(t("message.tooltip.doctor_m.removed_upgrade"));
            tooltip.add(t(item == items.ENERGY_UPGRADE_MODULE
                    ? "message.tooltip.doctor_m.energy_upgrade.desc"
                    : "message.tooltip.doctor_m.regeneration_module.desc"));
            return;
        }

        if (item == items.COFFEE_MACHINE) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.tooltip.doctor_m.coffee_machine"));
        }

        // 玩偶（直接显示）
        else if (item == items.DOLL_JIN_MARY) {
            tooltip.add(t("message.tooltip.doctor_m.doll_jin_mary"));
        }
        else if (item == items.DOLL_SMALLMOSS_OLD) {
            tooltip.add(t("message.tooltip.doctor_m.doll_smallmoss_old"));
        }
        else if (item == items.DOLL_SIGEERTE) {
            tooltip.add(t("message.tooltip.doctor_m.doll_sigeerte"));
        }
        else if (item == items.DOLL_TSINAFS_BCIM) {
            tooltip.add(t("message.tooltip.doctor_m.doll_tsinafs_bcim"));
        }
        else if (item == items.DOLL_TC020) {
            tooltip.add(t("message.tooltip.doctor_m.doll_tc020"));
        }
        else if (item == items.DOLL_ASDJDFK) {
            tooltip.add(t("message.tooltip.doctor_m.doll_asdjdfk"));
        }

        else if (item == items.OXYGEN_CHARGER) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.doctor_m.oxygen_charger"));
        }

        else if (item == items.UNDERWATER_OXYGEN_GENERATOR) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.doctor_m.underwater_oxygen_generator"));
        }

        else if (item == items.EYE_OF_HARMONY_OBELISK) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.tooltip.doctor_m.eye_of_harmony_obelisk"));
        }

        else if (item == items.TOYOTA_SPINNING_ROTOR) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip, t("message.tooltip.doctor_m.toyota_spinning_rotor"));
        }

        else if (stack.isOf(items.SEAL_OF_THE_HIGH_COUNCIL)) {
            tooltip.add(t("message.doctor_m.tip.not.done"));
        }
    }

    private static Text t(String key) {
        return Text.translatable(key).formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
    }
}