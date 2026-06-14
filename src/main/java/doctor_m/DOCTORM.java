package doctor_m;

import dev.amble.lib.container.RegistryContainer;
import doctor_m.Item.item_group;
import doctor_m.entities.entities;
import doctor_m.Item.items;
import doctor_m.wolrd_data.PocketWatchFunction;
import doctor_m.wolrd_data.TimeKeyFunction;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";
    public static final Identifier TOGGLE_PASSIVE_A = new Identifier(MOD_ID, "toggle_passive_a");
    public static final Identifier TOGGLE_PASSIVE_B = new Identifier(MOD_ID, "toggle_passive_b");

    @Override
    public void onInitialize() {
        RegistryContainer.register(items.class, MOD_ID);
        items.registerAbilities();
        item_group.registerItems();
        entities.registerAttributes();
        doctor_m.dimension.trenzalore.register();
        TimeKeyFunction.register();
        PocketWatchFunction.register();

    }
}