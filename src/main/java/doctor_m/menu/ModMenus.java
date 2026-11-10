package doctor_m.menu;

import doctor_m.DOCTORM;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public class ModMenus {

    public static final ScreenHandlerType<SARGUI> SAR_WORKBENCH_MENU =
            Registry.register(Registries.SCREEN_HANDLER, DOCTORM.id("sar_workbench"),
                    new ScreenHandlerType<>(SARGUI::new, FeatureFlags.VANILLA_FEATURES));
    public static final ScreenHandlerType<ATCLModificationSystemScreenHandler> ATCL_MODIFICATION_SYSTEM_MENU =
            Registry.register(Registries.SCREEN_HANDLER, DOCTORM.id("atcl_modification_system"),
                    new ScreenHandlerType<>(ATCLModificationSystemScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static void register() {
    }
}