package doctor_m;

import doctor_m.block.MomentBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem; // 1. 导入 BlockItem
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

public class DOCTORM implements ModInitializer {

    public static final String MOD_ID = "doctor_m";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // === 声明所有要注册的对象 ===
    // 物品
    public static final Item CONSOLE_FRAGMENT = new Item(new Item.Settings().maxCount(64));
    public static final Item PRIMORDIAL_RIFT_ITEM = new Item(new Item.Settings().maxCount(64));

    // 方块
    public static final Block MOMENT_BLOCK = new MomentBlock(
            AbstractBlock.Settings.create().strength(1.5f, 6.0f)
    );
    // 2. 声明方块对应的物品！！！（这是你缺少的关键部分）
    public static final Item MOMENT_BLOCK_ITEM = new BlockItem(MOMENT_BLOCK, new Item.Settings());

    @Override
    public void onInitialize() {
        LOGGER.info("开始初始化 Doctor M ...");

        // === 在这里集中注册所有内容 ===
        // 注册普通物品
        Registry.register(Registries.ITEM, id("console_fragment"), CONSOLE_FRAGMENT);
        Registry.register(Registries.ITEM, id("primordial_rift"), PRIMORDIAL_RIFT_ITEM);

        // 3. 将方块和方块物品的注册移到 onInitialize 方法内！！！
        Registry.register(Registries.BLOCK, id("moment"), MOMENT_BLOCK);
        Registry.register(Registries.ITEM, id("moment"), MOMENT_BLOCK_ITEM); // 注册方块物品

        // 注册资源重载监听器
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return id("ait_zh_loader");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        LOGGER.info("正在重载 Doctor M 自定义资源...");
                        Map<Identifier, Resource> resources = manager.findResources("my_resource_folder", path -> path.getPath().endsWith(".json"));
                        for (Identifier id : resources.keySet()) {
                            LOGGER.info("找到资源文件: {}", id);
                            try {
                                Resource resource = manager.getResource(id).orElse(null);
                                if (resource != null) {
                                    try (InputStream stream = resource.getInputStream()) {
                                        // TODO: 处理你的JSON流
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.error("加载资源 JSON 时出错: " + id, e);
                            }
                        }
                        LOGGER.info("Doctor M 自定义资源重载完成。");
                    }
                });

        LOGGER.info("Doctor M 已加载完成！");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}