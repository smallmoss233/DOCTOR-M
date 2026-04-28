package doctor_m.Item;

import doctor_m.entities.entities;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class items {
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

    public static void registerItems() {
    Registry.register(Registries.ITEM, id("photo"), PHOTO);
    Registry.register(Registries.ITEM, id("type_103_tardis_spawn"), TYPE_103_SPAWN);
    Registry.register(Registries.ITEM, id("evereye_spawn"), EVEREYE_SPAWN);
    }

    private static Identifier id(String path) {
        return new Identifier("doctor_m", path);
    }
}
