package doctor_m;

import doctor_m.CoffeeMachine.CoffeeMachineBlock;
import doctor_m.Weapon.DeMatGunItem;
import doctor_m.Weapon.RassilonKeyItem;
import doctor_m.block.entity.DOCTORMBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 声明要注册的东西
    public static final Block COFFEE_MACHINE_BLOCK = new CoffeeMachineBlock(AbstractBlock.Settings.create().strength(1.5f, 6.0f));
    public static final Block MOMENT_BLOCK = new Block(AbstractBlock.Settings.create().strength(1.5f, 6.0f));
    public static final Item DE_MAT_GUN = new DeMatGunItem(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));
    public static final Item RASSILON_KEY = new RassilonKeyItem(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC));
    public static final ItemGroup DOCTOR_M_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(RASSILON_KEY))
            .displayName(Text.translatable("itemGroup.doctor_m"))
            .entries((context, entries) -> {
                // 将你的物品全部添加到这个组
                entries.add(DE_MAT_GUN);
                entries.add(RASSILON_KEY);
                entries.add(COFFEE_MACHINE_BLOCK);
                entries.add(MOMENT_BLOCK);
            })
            .build();

    @Override
    public void onInitialize() {
        LOGGER.info("注册 Doctor M 物品/方块...");

        // 注册方块和物品
        Registry.register(Registries.BLOCK, id("coffee_machine_block"), COFFEE_MACHINE_BLOCK);
        Registry.register(Registries.ITEM, id("coffee_machine_block"),
                new BlockItem(COFFEE_MACHINE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, id("moment"), MOMENT_BLOCK);
        Registry.register(Registries.ITEM, id("moment"),
                new BlockItem(MOMENT_BLOCK, new Item.Settings()));

        Registry.register(Registries.ITEM, id("de_mat_gun"), DE_MAT_GUN);
        Registry.register(Registries.ITEM, id("rassilon_key"), RASSILON_KEY);
        // 物品组
        Registry.register(Registries.ITEM_GROUP, id("doctor_m_group"), DOCTOR_M_GROUP);

        DOCTORMBlockEntities.registerBlockEntities();

        LOGGER.info("Doctor M 已加载！");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}