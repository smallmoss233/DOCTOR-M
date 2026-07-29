package doctor_m.Item.data_itme.TimeKyeFragment;

import dev.emi.trinkets.api.TrinketItem;
import doctor_m.Item.KeytoTime;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EternalCrystalItem extends TrinketItem implements KeytoTime {

    public EternalCrystalItem(Settings settings) {
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