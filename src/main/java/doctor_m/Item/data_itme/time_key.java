package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class time_key extends TrinketItem {

    public time_key(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.doctor_m.time_key.tooltip.line1"));
        tooltip.add(Text.translatable("item.doctor_m.time_key.tooltip.line2"));
        tooltip.add(Text.translatable("doctor_m.tip.not.done"));
    }
}