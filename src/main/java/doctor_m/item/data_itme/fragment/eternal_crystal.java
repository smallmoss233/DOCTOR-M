package doctor_m.item.data_itme.fragment;

import dev.emi.trinkets.api.TrinketItem;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class eternal_crystal extends TrinketItem {

    public eternal_crystal(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 简单的饰品描述（可自定义）
        Text description = Text.translatable("message.doctor_m.eternal_crystal.desc");
        TooltipHelper.addWrappedTooltip(tooltip, description);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.eternal_crystal.detail")
        );
    }
}