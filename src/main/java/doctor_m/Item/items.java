package doctor_m.Item;

import dev.amble.lib.container.impl.ItemContainer;
import doctor_m.Item.data_itme.*;
import doctor_m.Item.data_itme.TimeKyeFragment.EternalCrystalItem;
import doctor_m.Item.data_itme.TimeKyeFragment.PocketWatchItem;
import doctor_m.Item.data_itme.TimeKyeFragment.RelicGemItem;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.entities.Entities;
import doctor_m.module.space_plus.OxygenTankItem;
import doctor_m.module.space_plus.block.ModBlocks;
import doctor_m.util.creativity.PercentageDamageHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Rarity;

public class items extends ItemContainer {

    //时间钥匙
    public static final Item TIME_KEY = new TimeKeyItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item POCKET_WATCH = new PocketWatchItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item RELIC_GEM = new RelicGemItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item ETERNAL_CRYSTAL = new EternalCrystalItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    //道具
    public static final Item SHIELD_CORE = new ShieldCoreItem(new Item.Settings().maxCount(1));
    public static final Item ENERGY_UPGRADE_MODULE = new Item(new Item.Settings().maxCount(1));
    public static final Item REGENERATION_MODULE = new Item(new Item.Settings().maxCount(1));
    public static final Item FORCE_FIELD_SHIELD = new ForceFieldShieldItem(new Item.Settings().maxCount(1));
    public static final Item VORTEX_MANIPULATOR = new VortexManipulatorItem(new Item.Settings().maxCount(1));
    public static final Item TRACER = new TracerItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item TOYMAKER_HAMMER = new ToymakerHammerItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    //氧气相关
    public static final Item OXYGEN_CHARGER = new BlockItem(ModBlocks.OXYGEN_CHARGER, new Item.Settings());
    public static final Item UNDERWATER_OXYGEN_GENERATOR = new BlockItem(ModBlocks.UNDERWATER_OXYGEN_GENERATOR, new Item.Settings());
    public static final Item OXYGEN_TANK = new OxygenTankItem(new Item.Settings().maxCount(1));

    //实体相关
    public static final Item TYPE_103_SPAWN = new SpawnEggItem(
            Entities.TYPE_103_TARDIS,
            0xFFFFFFFF,
            0xFFFFFFFF,
            new Item.Settings()
    );
    public static final Item EVEREYE_SPAWN = new SpawnEggItem(
            Entities.TYPE_103W_EVEREYE,
            0xFFFFFFFF,
            0xFFFFFFFF,
            new Item.Settings()
    );
    public static final Item DE_MAT_GUN = new DeMatGunItem(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));

    public static final Item RASSILON_KEY = new RassilonKeyItem(new Item.Settings()
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