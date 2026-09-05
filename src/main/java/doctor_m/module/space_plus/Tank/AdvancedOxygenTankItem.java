package doctor_m.module.space_plus.Tank;

import doctor_m.config.ConfigManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class AdvancedOxygenTankItem extends OxygenTankItem {
    public AdvancedOxygenTankItem(Settings settings) {
        super(settings);
    }

    @Override
    public double getMaxOxygen() {
        return super.getMaxOxygen() * ConfigManager.getConfig().advancedOxygenTankCapacityMultiplier;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("message.doctor_m.advanced_oxygen_tank").formatted(Formatting.AQUA));
    }
}