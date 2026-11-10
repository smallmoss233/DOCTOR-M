package doctor_m.module.creativity.creativity_data.SAR.atcl_models;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class ATCLConstructionModifiedModel extends Item {
    public ATCLConstructionModifiedModel(Settings settings){
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.doctor_m.atcl_damage_up.desc").formatted(Formatting.GRAY));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
