package doctor_m.Weapon;

import dev.amble.ait.module.gun.core.item.BaseGunItem;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DeMatGunItem extends BaseGunItem {

    public DeMatGunItem(Settings settings) {
        super(settings);
        // 如果你需要额外的初始化，可以在这里写
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.doctor_m.de_mat_gun.tooltip.line1")
                .formatted(Formatting.WHITE, Formatting.BOLD));
        MutableText line2 = Text.translatable("item.doctor_m.de_mat_gun.tooltip.line2");
        line2.setStyle(Style.EMPTY
                .withColor(Formatting.GRAY)
                .withItalic(true));
        tooltip.add(line2);
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("doctor_m.tip.not.done"));
    }

    @Override
    public double getMaxAmmo() {
        return 128;
    }
    @Override
    public int getCooldown() {
        return 5;
    }
    @Override
    public float getAimDeviation(boolean isAds) {
        return isAds ? 0.15f : 1.2f;
    }
}