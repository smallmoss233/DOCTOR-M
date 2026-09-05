package doctor_m.module.space_plus.Tank;

import doctor_m.config.ConfigManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class SuperOxygenTankItem extends OxygenTankItem {
    public SuperOxygenTankItem(Settings settings) {
        super(settings);
    }

    @Override
    public double getMaxOxygen() {
        double base = super.getMaxOxygen(); // 普通氧气瓶容量
        double advanced = base * ConfigManager.getConfig().advancedOxygenTankCapacityMultiplier;
        return advanced * ConfigManager.getConfig().superOxygenTankCapacityMultiplier;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("message.doctor_m.super_oxygen_tank").formatted(Formatting.LIGHT_PURPLE));
    }
}