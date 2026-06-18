package doctor_m.Item;

import dev.amble.lib.container.impl.ItemContainer;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;

import static doctor_m.Item.data_Industry_items.SteelArmorMaterial.STEEL_ARMOR_MATERIAL;

public class Industry_items extends ItemContainer {
    public static final Item STEEL_INGOT = new Item(new Item.Settings());
    public static final Item STEEL_SHEET = new Item(new Item.Settings());
    public static final Item STEEL_HELMET = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings());
    public static final Item STEEL_CHESTPLATE = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings());
    public static final Item STEEL_LEGGINGS = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings());
    public static final Item STEEL_BOOTS = new ArmorItem(STEEL_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings());
}
