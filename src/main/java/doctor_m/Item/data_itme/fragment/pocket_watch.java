package doctor_m.Item.data_itme.fragment;

import dev.emi.trinkets.api.TrinketItem;  // 改为 TrinketItem
import doctor_m.util.javautil.ShiftTooltipInvoker;
import doctor_m.util.javautil.TooltipHelper;
import doctor_m.world_data.PocketWatchFunction;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
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
                kotlin.Pair<Integer, Integer> parts = PocketWatchFunction.getRemainingTimeParts(remaining);
                int minutes = parts.getFirst();
                int seconds = parts.getSecond();
                Text longDescription = Text.translatable("message.doctor_m.pocket_watch.cooldown", minutes, seconds);
                TooltipHelper.addWrappedTooltip(tooltip, longDescription);
            }
        }

        Text longDescription = Text.translatable("message.doctor_m.pocket_watch.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.pocket_watch.detail")
        );
    }
}