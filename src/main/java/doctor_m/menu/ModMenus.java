package doctor_m.menu;

import doctor_m.DOCTORM;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public class ModMenus {

    // 注册你工作台的 MenuType
    public static final ScreenHandlerType<SARGUI> SAR_WORKBENCH_MENU =
            Registry.register(Registries.SCREEN_HANDLER, DOCTORM.id("sar_workbench"),
                    new ScreenHandlerType<>(SARGUI::new, FeatureFlags.VANILLA_FEATURES));

    public static void register() {
    }
}