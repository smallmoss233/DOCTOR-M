package doctor_m.Item.data_itme.fragment;

import dev.emi.trinkets.api.TrinketItem;  // 改为 TrinketItem
import doctor_m.util.ShiftTooltipInvoker;
import doctor_m.util.TooltipHelper;
import world_data.PocketWatchFunction;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class pocket_watch extends TrinketItem {

    public pocket_watch(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(PocketWatchFunction.COOLDOWN_KEY)) {
            long cooldownEnd = nbt.getLong(PocketWatchFunction.COOLDOWN_KEY);
            long now = System.currentTimeMillis();
            if (now < cooldownEnd) {
                long remaining = cooldownEnd - now;
                String timeStr = PocketWatchFunction.formatRemainingTime(remaining);
                tooltip.add(Text.translatable("message.doctor_m.pocket_watch.cooldown", timeStr).formatted(Formatting.GRAY));
            }
        }

        // 需要自动换行的长文本
        Text longDescription = Text.translatable("message.doctor_m.pocket_watch.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.pocket_watch.detail")
        );
    }
}