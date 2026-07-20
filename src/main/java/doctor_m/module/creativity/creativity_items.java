package doctor_m.module.creativity;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.module.creativity.creativity_data.tlipoca_scythe;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.util.creativity.PercentageDamageHelper;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

public class creativity_items extends ItemContainer {

    // 特莉波卡的镰刀
    public static final Item TLIPOCA_SCYTHE = new tlipoca_scythe(
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
    );
    public static void registerAbilities() {
        ModConfig config = ConfigManager.getConfig();

        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                config.tlipocaScytheDamage,
                config.tlipocaScytheMultiplier,
                config.tlipocaScytheExtra,
                player -> {
                    return player.getMainHandStack().getItem() instanceof tlipoca_scythe ||
                            player.getOffHandStack().getItem() instanceof tlipoca_scythe;
                }
        ));
    }
}