package doctor_m.Item.data_itme;

import dev.amble.ait.core.item.KeyItem;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RassilonKeyItem extends KeyItem {   // 继承 KeyItem

    public RassilonKeyItem(Settings settings) {
        super(settings, Protocols.SNAP, Protocols.HAIL, Protocols.PERCEPTION, Protocols.SKELETON);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Text longDescription = Text.translatable("message.doctor_m.rassilon_key.tooltip.line");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
        super.appendTooltip(stack, world, tooltip, context);
    }
}