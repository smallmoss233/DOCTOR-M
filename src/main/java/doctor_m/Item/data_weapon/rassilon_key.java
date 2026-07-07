package doctor_m.Item.data_weapon;

import doctor_m.util.javautil.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class rassilon_key extends Item {
    public rassilon_key(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            // 从需要自动换行的长文本
    Text longDescription = Text.translatable("message.doctor_m.rassilon_key.tooltip.line");
    TooltipHelper.addWrappedTooltip(tooltip, longDescription);
    tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
    }
}