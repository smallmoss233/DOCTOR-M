package doctor_m.Weapon;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import java.util.List;

public class RassilonKeyItem extends Item {
    public RassilonKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        // 第一条提示
        tooltip.add(Text.translatable("item.doctor_m.rassilon_key.tooltip.line1").formatted(net.minecraft.util.Formatting.GREEN));
        // 第二条提示（带绿色）
        tooltip.add(Text.translatable("item.doctor_m.rassilon_key.tooltip.line2"));
    }
}