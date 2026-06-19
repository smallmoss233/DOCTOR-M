package doctor_m.module.Industry;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.module.space.ModBlocks;
import doctor_m.module.space.OxygenTankItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;


public class Industry_items extends ItemContainer {
    public static final Item OXYGEN_CHARGER_ITEM = new BlockItem(ModBlocks.OXYGEN_CHARGER, new Item.Settings());
    public static final Item OXYGEN_TANK = new OxygenTankItem(new Item.Settings().maxCount(1));
    public static final Item STEEL_INGOT = new Item(new Item.Settings());
    public static final Item STEEL_SHEET = new Item(new Item.Settings());
}