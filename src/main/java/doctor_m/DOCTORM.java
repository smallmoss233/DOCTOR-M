package doctor_m;

import doctor_m.block.MomentBlock;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 注册的物品和方块
    public static final Item CONSOLE_FRAGMENT = new Item(new Item.Settings().maxCount(64));
    public static final Item PRIMORDIAL_RIFT_ITEM = new Item(new Item.Settings().maxCount(64));
    public static final Block MOMENT_BLOCK = new MomentBlock(AbstractBlock.Settings.create().strength(1.5f, 6.0f));
    public static final Item MOMENT_BLOCK_ITEM = new BlockItem(MOMENT_BLOCK, new Item.Settings());

    @Override
    public void onInitialize() {
        LOGGER.info("开始初始化 Doctor M ...");

        // 注册物品和方块
        Registry.register(Registries.ITEM, id("console_fragment"), CONSOLE_FRAGMENT);
        Registry.register(Registries.ITEM, id("primordial_rift"), PRIMORDIAL_RIFT_ITEM);
        Registry.register(Registries.BLOCK, id("moment"), MOMENT_BLOCK);
        Registry.register(Registries.ITEM, id("moment"), MOMENT_BLOCK_ITEM);

        doctor_m.worldgen.ChunkEventListener.initialize();

        LOGGER.info("Doctor M 已加载");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}