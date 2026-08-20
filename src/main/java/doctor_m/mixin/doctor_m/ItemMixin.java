package doctor_m.mixin.doctor_m;

import dev.amble.ait.core.item.SonicItem;
import doctor_m.module.sonic_plus.UpgradeModuleManager;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
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

import java.util.List;

import static doctor_m.Item.items.*;

@Mixin(Item.class)
public class ItemMixin {

    private static final Identifier OXYGEN_CHARGER_ID = new Identifier("doctor_m", "oxygen_charger");
    private static final Identifier UNDERWATER_OXYGEN_GENERATOR_ID = new Identifier("doctor_m", "underwater_oxygen_generator");
    private static final Identifier EYE_OF_HARMONY_OBELISK_ID = new Identifier("doctor_m", "eye_of_harmony_obelisk");
    private static final Identifier TOYOTA_SPINNING_ROTOR_ENTITY_ID = new Identifier("doctor_m", "toyota_spinning_rotor");


    // 能量再生核心恢复能量
    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void doctor_m$regenerateFuel(ItemStack stack, World world, Entity entity,
                                         int slot, boolean selected, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!(stack.getItem() instanceof SonicItem sonic)) return;
        if (!UpgradeModuleManager.hasRegenerationModule(stack)) return;

        if (world.getTime() % 100 != 0) return;

        double current = sonic.getCurrentFuel(stack);
        double max = sonic.getMaxFuel(stack);

        if (current < max) {
            sonic.addFuel(1.0, stack);
        }
    }

    //物品提示
    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void doctor_m$moduleTooltips(ItemStack stack, @Nullable World world,
                                         List<Text> tooltip, TooltipContext context,
                                         CallbackInfo ci) {
        Identifier id = Registries.ITEM.getId(stack.getItem());

        // 音速起子升级模块提示
        if (id.equals(UpgradeModuleManager.ENERGY_UPGRADE) || id.equals(UpgradeModuleManager.REGENERATION_MODULE)) {
            tooltip.add(Text.translatable("message.tooltip.doctor_m.upgrade")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("message.tooltip.doctor_m.removed_upgrade")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));

            if (id.equals(UpgradeModuleManager.ENERGY_UPGRADE)) {
                tooltip.add(Text.translatable("message.tooltip.doctor_m.energy_upgrade.desc")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            } else {
                tooltip.add(Text.translatable("message.tooltip.doctor_m.regeneration_module.desc")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            }
        }

        // 氧气补充机
        if (id.equals(OXYGEN_CHARGER_ID)) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip,
                    Text.translatable("message.doctor_m.oxygen_charger"));
        }

        // 水下制氧机
        if (id.equals(UNDERWATER_OXYGEN_GENERATOR_ID)) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip,
                    Text.translatable("message.doctor_m.underwater_oxygen_generator"));
        }

        // 和谐之眼方尖碑
        if (id.equals(EYE_OF_HARMONY_OBELISK_ID)) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip,
                    Text.translatable("message.tooltip.doctor_m.eye_of_harmony_obelisk"));
        }

        // 穹顶转子
        if (id.equals(TOYOTA_SPINNING_ROTOR_ENTITY_ID)) {
            ShiftTooltipInvoker.addShiftTooltip(tooltip,
                    Text.translatable("message.tooltip.doctor_m.toyota_spinning_rotor"));
        }

        // 至高议会勋章
        if (stack.isOf(SEAL_OF_THE_HIGH_COUNCIL)) {
            tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
        }
    }
}