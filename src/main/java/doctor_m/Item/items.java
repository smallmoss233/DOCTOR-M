package doctor_m.Item;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.entities.entities;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;

public class items extends ItemContainer {

    public static final Item PHOTO = new Item(new Item.Settings());

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
}