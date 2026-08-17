package doctor_m.module.creativity;

import doctor_m.Item.stcs.STCAItem;
import doctor_m.Item.stcs.STCHItem;
import doctor_m.Item.stcs.STCLItem;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
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
    public static final Item STCA = new STCAItem();
    public static final Item STCH = new STCHItem();
    public static final Item STCL = new STCLItem();

    public static void registerAbilities() {
        ModConfig config = ConfigManager.getConfig();

        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                ModConfig.tlipocaScytheDamage,
                ModConfig.tlipocaScytheMultiplier,
                ModConfig.tlipocaScytheExtra,
                player -> {
                    return player.getMainHandStack().getItem() instanceof TlipocaScytheItem ||
                            player.getOffHandStack().getItem() instanceof TlipocaScytheItem;
                }
        ));
    }
}