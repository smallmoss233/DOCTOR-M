package doctor_m.client.Config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import net.minecraft.client.gui.screen.Screen;

/**
 * Mod Menu 集成入口。
 *
 * <p>只在玩家安装了 Mod Menu 时被加载——没有 Mod Menu 时，
 * Fabric Loader 不会触发 {@code modmenu} entrypoint，因此不会触发任何 ClassNotFound。
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::createConfigScreen;
    }

    private static Screen createConfigScreen(Screen parent) {
        ModConfig cfg = ConfigManager.getConfig();
        return ConfigScreenBuilder.build(parent, cfg);
    }
}