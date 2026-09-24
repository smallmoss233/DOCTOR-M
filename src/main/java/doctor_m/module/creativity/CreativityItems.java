package doctor_m.module.creativity;

import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.module.creativity.creativity_data.SAR.SARAItem;
import doctor_m.module.creativity.creativity_data.SAR.SARHItem;
import doctor_m.module.creativity.creativity_data.SAR.SARLItem;
import doctor_m.module.creativity.creativity_data.Tlipoca.TlipocaScytheItem;
import doctor_m.util.creativity.PercentageDamageHelper;
import net.minecraft.item.Item;
import net.minecraft.util.Rarity;

public class CreativityItems {

    // 特莉波卡的镰刀
    public static final Item TLIPOCA_SCYTHE = new TlipocaScytheItem(
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
    );

    // STCS系列武器
    public static final Item STCA = new SARAItem();
    public static final Item STCH = new SARHItem();
    public static final Item STCL = new SARLItem();

    public static void registerAbilities() {
        ModConfig config = ConfigManager.getConfig();

        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                config.tlipocaScytheDamage,
                config.tlipocaScytheMultiplier,
                config.tlipocaScytheExtra,
                player -> {
                    return player.getMainHandStack().getItem() instanceof TlipocaScytheItem ||
                            player.getOffHandStack().getItem() instanceof TlipocaScytheItem;
                }
        ));
    }
}