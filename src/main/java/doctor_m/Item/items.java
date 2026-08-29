package doctor_m.Item;

import doctor_m.Item.data_item.*;
import doctor_m.Item.data_item.KeytoTimeFragment.EternalCrystalItem;
import doctor_m.Item.data_item.KeytoTimeFragment.PocketWatchItem;
import doctor_m.Item.data_item.KeytoTimeFragment.RelicGemItem;
import doctor_m.block.ModBlocks;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.entities.Entities;
import doctor_m.module.space_plus.OxygenTankItem;
import doctor_m.util.creativity.PercentageDamageHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Rarity;

public class items {

    //时间钥匙相关
    public static final Item KEY_TO_TIME = new KeytoTimeItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
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
    public static final Item SEAL_OF_THE_HIGH_COUNCIL = new Item(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    //去物质枪和拉斯隆之钥
    public static final Item DE_MAT_GUN = new DeMatGunItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));
    public static final Item KEY_OF_RASSILON = new KeyofRassilonItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    //氧气相关
    public static final Item OXYGEN_CHARGER = new BlockItem(ModBlocks.OXYGEN_CHARGER, new Item.Settings());
    public static final Item UNDERWATER_OXYGEN_GENERATOR = new BlockItem(ModBlocks.UNDERWATER_OXYGEN_GENERATOR, new Item.Settings());
    public static final Item OXYGEN_TANK = new OxygenTankItem(new Item.Settings().maxCount(1));

    //穹顶转子和咖啡机
    public static final Item TOYOTA_SPINNING_ROTOR = new BlockItem(ModBlocks.TOYOTA_SPINNING_ROTOR, new Item.Settings());
    public static final Item COFFEE_MACHINE = new BlockItem(ModBlocks.COFFEE_MACHINE, new Item.Settings());

    //玩偶
    public static final Item DOLL_JIN_MARY = new BlockItem(ModBlocks.DOLL_JIN_MARY, new Item.Settings());
    public static final Item DOLL_SMALLMOSS_OLD = new BlockItem(ModBlocks.DOLL_SMALLMOSS_OLD, new Item.Settings());
    public static final Item DOLL_TC020 = new BlockItem(ModBlocks.DOLL_TC020, new Item.Settings());
    public static final Item DOLL_ASDJDFK =new BlockItem(ModBlocks.DOLL_ASDJDFK, new Item.Settings());
    public static final Item DOLL_SIGEERTE =new BlockItem(ModBlocks.DOLL_SIGEERTE, new Item.Settings());
    public static final Item DOLL_TSINAFS_BCIM =new BlockItem(ModBlocks.DOLL_TSINAFS_BCIM, new Item.Settings());
    public static final Item DOLL_ASNIT_PNQING =new BlockItem(ModBlocks.DOLL_ASNIT_PNQING, new Item.Settings());
    public static final Item DOLL_TIANX =new BlockItem(ModBlocks.DOLL_TIANX, new Item.Settings());
    public static final Item DOLL_KILIN_MUS =new BlockItem(ModBlocks.DOLL_KILIN_MUS, new Item.Settings());

    //和谐之眼
    public static final Item EYE_OF_HARMONY_OBELISK = new BlockItem(ModBlocks.EYE_OF_HARMONY_OBELISK, new Item.Settings());

    //刷怪蛋
    public static final Item TYPE_103_SPAWN = new SpawnEggItem(
            Entities.TYPE_103_TARDIS, 0xFFFFFFFF, 0xFFFFFFFF, new Item.Settings());
    public static final Item MARIAN_JIN_SPAWN = new SpawnEggItem(
            Entities.MARIAN_JIN, 0xFFFFFFFF, 0xFFFFFFFF, new Item.Settings());

    //强制百分比扣血
    public static void registerAbilities() {
        ModConfig config = ConfigManager.getConfig();

        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                ModConfig.KeytoTimeDamage,
                ModConfig.KeytoTimeMultiplier,
                ModConfig.KeytoTimeExtra,
                PercentageDamageHelper.hasAnyOfItems(KEY_TO_TIME)
        ));

        new PercentageDamageHelper(new PercentageDamageHelper.Config(
                ModConfig.eternalCrystalDamage,
                ModConfig.eternalCrystalMultiplier,
                ModConfig.eternalCrystalExtra,
                PercentageDamageHelper.hasAnyOfItems(ETERNAL_CRYSTAL)
        ));
    }
}