package doctor_m.Item;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.Item.data_weapon.de_mat_gun;
import doctor_m.Item.data_weapon.rassilon_key;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

public class itmes_weapon extends ItemContainer {

    public static final Item DE_MAT_GUN = new de_mat_gun(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));

    public static final Item RASSILON_KEY = new rassilon_key(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));
}