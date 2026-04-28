package doctor_m.Item.data_weapon;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import java.util.List;

public class rassilon_key extends Item {
    public rassilon_key(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.doctor_m.rassilon_key.tooltip.line1"));
        tooltip.add(Text.translatable("item.doctor_m.rassilon_key.tooltip.line2"));
        tooltip.add(Text.translatable("doctor_m.tip.not.done"));
    }
}