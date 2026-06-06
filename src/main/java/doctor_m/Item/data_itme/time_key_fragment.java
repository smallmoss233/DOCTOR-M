package doctor_m.Item.data_itme;

import doctor_m.util.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class time_key_fragment extends Item {

    public time_key_fragment(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 读取需要自动换行的长文本（请确保语言文件中有这个键值）
        Text longDescription = Text.translatable("txt.doctor_m.time_key_fragment.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription, 8);
        // 添加未完成提示
        tooltip.add(Text.translatable("txt.doctor_m.tip.not.done"));
    }
}