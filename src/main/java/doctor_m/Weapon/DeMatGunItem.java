package doctor_m.Weapon;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DeMatGunItem extends Item {

    public DeMatGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // 添加空行分隔
        tooltip.add(Text.empty());

        // 添加第一行提示（加粗，白色）
        tooltip.add(Text.translatable("item.doctor_m.de_mat_gun.tooltip.line1")
                .formatted(Formatting.WHITE, Formatting.BOLD));

        // 添加第二行提示（斜体，淡灰色）
        MutableText line2 = Text.translatable("item.doctor_m.de_mat_gun.tooltip.line2");
        line2.setStyle(Style.EMPTY
                .withColor(Formatting.GRAY)
                .withItalic(true));
        tooltip.add(line2);

        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public Text getName(ItemStack stack) {
        // 返回紫色的物品名称，加粗
        return Text.translatable(this.getTranslationKey(stack))
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
    }
}