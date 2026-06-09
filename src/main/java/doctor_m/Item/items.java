package doctor_m.Item;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.Item.data_itme.time_key;
import doctor_m.Item.data_itme.fragment.pocket_watch;
import doctor_m.Item.data_weapon.de_mat_gun;
import doctor_m.Item.data_weapon.rassilon_key;
import doctor_m.entities.entities;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Rarity;

public class items extends ItemContainer {

    public static final Item TIME_KEY = new time_key(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item POCKET_WATCH = new pocket_watch(new Item.Settings());

    public static final Item TYPE_103_SPAWN = new SpawnEggItem(
            entities.TYPE_103_TARDIS,
            0xFFFFFFFF,
            0xFFFFFFFF,
            new Item.Settings()
    );
    public static final Item EVEREYE_SPAWN = new SpawnEggItem(
            entities.TYPE_103W_EVEREYE,
            0xFFFFFFFF,
            0xFFFFFFFF,
            new Item.Settings()
    );
    public static final Item DE_MAT_GUN = new de_mat_gun(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));

    public static final Item RASSILON_KEY = new rassilon_key(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));
}