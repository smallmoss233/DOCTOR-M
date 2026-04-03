package doctor_m;

import doctor_m.Entities.ModEntities;
import doctor_m.Item.Items;
import doctor_m.Weapon.Weapon;
import net.fabricmc.api.ModInitializer;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";

    @Override
    public void onInitialize() {
        ModEntities.register();
        Weapon.registerItems();
        ModEntities.register();
        Items.registerItems();
        DOCTORMItem_group.registerItems();
    }
}