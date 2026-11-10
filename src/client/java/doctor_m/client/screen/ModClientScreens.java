package doctor_m.client.screen;

import doctor_m.menu.ModMenus;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ModClientScreens {
    public static void register() {
        HandledScreens.register(ModMenus.ATCL_MODIFICATION_SYSTEM_MENU, ATCLModificationSystemScreen::new);
    }
}