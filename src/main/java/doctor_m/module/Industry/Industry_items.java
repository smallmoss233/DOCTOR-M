package doctor_m.module.Industry;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.module.space.ModBlocks;
import doctor_m.module.space.OxygenTankItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

import static doctor_m.module.Industry.data_Industry.SteelArmorMaterial.STEEL_ARMOR_MATERIAL;

public class Industry_items extends ItemContainer {
    public static final Item OXYGEN_CHARGER_ITEM = new BlockItem(ModBlocks.OXYGEN_CHARGER, new Item.Settings());
    public static final Item OXYGEN_TANK = new OxygenTankItem(new Item.Settings().maxCount(1));
    public static final Item STEEL_INGOT = new Item(new Item.Settings());
    public static final Item STEEL_SHEET = new Item(new Item.Settings());
    public static final Item STEEL_HELMET = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings().maxCount(1));
    public static final Item STEEL_CHESTPLATE = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxCount(1));
    public static final Item STEEL_LEGGINGS = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings().maxCount(1));
    public static final Item STEEL_BOOTS = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings().maxCount(1));
}