package doctor_m;

import doctor_m.Item.item_group;
import doctor_m.entities.Entities;
import doctor_m.Item.items;
import doctor_m.weapon.weapon_itme;
import net.fabricmc.api.ModInitializer;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";

    @Override
    public void onInitialize() {
        Entities.register();
        weapon_itme.registerItems();
        Entities.register();
        items.registerItems();
        item_group.registerItems();
        doctor_m.dimension.trenzalore.register();
    }
}