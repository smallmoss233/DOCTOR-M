package doctor_m.Weapon;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class Weapon {
    public static final Item DE_MAT_GUN = new DeMatGunItem(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));
    public static final Item RASSILON_KEY = new RassilonKeyItem(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));

    public static void registerItems() {
        Registry.register(Registries.ITEM, id("de_mat_gun"), DE_MAT_GUN);
        Registry.register(Registries.ITEM, id("rassilon_key"), RASSILON_KEY);
    }

    private static Identifier id(String path) {
        return new Identifier("doctor_m", path);
    }
}