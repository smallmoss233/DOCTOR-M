package doctor_m;

import dev.amble.lib.container.RegistryContainer;
import doctor_m.Item.item_group;
import doctor_m.entities.entities;
import doctor_m.Item.items;
import doctor_m.Item.itmes_weapon;
import net.fabricmc.api.ModInitializer;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";

    @Override
    public void onInitialize() {
        RegistryContainer.register(itmes_weapon.class, MOD_ID);
        RegistryContainer.register(items.class, MOD_ID);
        item_group.registerItems();
        entities.registerAttributes();
        doctor_m.dimension.trenzalore.register();
    }
}