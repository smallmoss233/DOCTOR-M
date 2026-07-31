package doctor_m.Item.data_itme;

import dev.amble.ait.module.gun.core.item.BaseGunItem;
import doctor_m.util.creativity.DynamicColorHelper;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class DeMatGunItem extends BaseGunItem {

    public DeMatGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
    }

    @Override
    public void tryShoot(World world, Entity entity, boolean selected) {
    }

    @Override
    public double getMaxAmmo() {
        return 128;
    }

    @Override
    public int getCooldown() {
        return 60;
    }

    @Override
    public float getAimDeviation(boolean isAds) {
        return isAds ? 0.15f : 1.2f;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 原有的两行基本提示
        tooltip.add(Text.translatable("message.doctor_m.de_mat_gun.tooltip.line1")
                .formatted(Formatting.WHITE, Formatting.BOLD));
        MutableText line2 = Text.translatable("message.doctor_m.de_mat_gun.tooltip.line2");
        line2.setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
        tooltip.add(line2);
        //能量提示
        super.appendTooltip(stack, world, tooltip, context);
        //详情提示
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.de_mat_gun.detail")
        );
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(128, 0, 128),  // 紫色
                Color.GRAY,              // 灰色
                Color.GRAY,              // 灰色
                new Color(128, 0, 128),  // 紫色
                Color.GRAY,              // 灰色
                Color.GRAY               // 灰色
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 20000);
    }
}