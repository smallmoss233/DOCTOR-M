package doctor_m.Item;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.Item.data_itme.fragment.eternal_crystal;
import doctor_m.Item.data_itme.fragment.mystery_gem;
import doctor_m.Item.data_itme.time_key;
import doctor_m.Item.data_itme.fragment.pocket_watch;
import doctor_m.Item.data_weapon.de_mat_gun;
import doctor_m.Item.data_weapon.rassilon_key;
import doctor_m.entities.entities;
import doctor_m.module.ait_space_mixin.ModBlocks;
import doctor_m.module.ait_space_mixin.OxygenTankItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Rarity;
import doctor_m.util.PercentageDamageHelper;

public class items extends ItemContainer {

    //DW物品相关
    public static final Item TIME_KEY = new time_key(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item POCKET_WATCH = new pocket_watch(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item MYSTERY_GEM = new mystery_gem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item ETERNAL_CRYSTAL = new eternal_crystal(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

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
        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                20, 1.0, 5.0, PercentageDamageHelper.hasAnyOfItems(TIME_KEY)
        ));
        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                100, 0.5, 2.5, PercentageDamageHelper.hasAnyOfItems(ETERNAL_CRYSTAL)
        ));
    }
}