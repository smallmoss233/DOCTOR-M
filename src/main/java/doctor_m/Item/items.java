package doctor_m.Item;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.Item.data_itme.ShieldCoreItem;
import doctor_m.Item.data_itme.fragment.eternal_crystal;
import doctor_m.Item.data_itme.fragment.relic_gem;
import doctor_m.Item.data_itme.time_key;
import doctor_m.Item.data_itme.fragment.pocket_watch;
import doctor_m.Item.data_weapon.de_mat_gun;
import doctor_m.Item.data_weapon.rassilon_key;
import doctor_m.entities.entities;
import doctor_m.module.ait_space_mixin.ModBlocks;
import doctor_m.module.ait_space_mixin.OxygenTankItem;
import doctor_m.util.config.ConfigManager;
import doctor_m.util.config.ModConfig;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Rarity;
import doctor_m.util.javautil.PercentageDamageHelper;

public class items extends ItemContainer {

    //时间钥匙
    public static final Item TIME_KEY = new time_key(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item POCKET_WATCH = new pocket_watch(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item RELIC_GEM = new relic_gem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item ETERNAL_CRYSTAL = new eternal_crystal(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    public static final Item SHIELD_CORE = new ShieldCoreItem(new Item.Settings().maxCount(1));

    //氧气相关
    public static final Item OXYGEN_CHARGER_ITEM = new BlockItem(ModBlocks.OXYGEN_CHARGER, new Item.Settings());
    public static final Item OXYGEN_TANK = new OxygenTankItem(new Item.Settings().maxCount(1));

    //实体相关
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
    public static final Item DE_MAT_GUN = new de_mat_gun(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));

    public static final Item RASSILON_KEY = new rassilon_key(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));

    public static void registerAbilities() {
        ModConfig config = ConfigManager.getConfig();

        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                config.timeKeyDamage,
                config.timeKeyMultiplier,
                config.timeKeyExtra,
                PercentageDamageHelper.hasAnyOfItems(TIME_KEY)
        ));

        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                config.eternalCrystalDamage,
                config.eternalCrystalMultiplier,
                config.eternalCrystalExtra,
                PercentageDamageHelper.hasAnyOfItems(ETERNAL_CRYSTAL)
        ));
    }
}