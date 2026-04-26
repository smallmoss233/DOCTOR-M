package doctor_m.Item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class items {
    public static final Item PHOTO = new Item(new Item.Settings());

    public static void registerItems() {Registry.register(Registries.ITEM, id("photo"), PHOTO);}

    private static Identifier id(String path) {
        return new Identifier("doctor_m", path);
    }
}
