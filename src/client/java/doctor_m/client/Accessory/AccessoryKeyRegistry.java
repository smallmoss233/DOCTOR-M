package doctor_m.client.Accessory;

import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AccessoryKeyRegistry {
    private static final List<AccessoryKeyHandler> HANDLERS = new ArrayList<>();

    public static void register(AccessoryKeyHandler handler) {
        HANDLERS.add(handler);
        HANDLERS.sort(Comparator.comparingInt(AccessoryKeyHandler::getPriority));
    }

    public static void handleSkillKey(PlayerEntity player) {
        for (AccessoryKeyHandler handler : HANDLERS) {
            if (handler.isActive(player)) {
                handler.onSkillKey(player);
                if (handler.blocksOthers()) return;
            }
        }
    }

    public static void handleCoreKey(PlayerEntity player) {
        for (AccessoryKeyHandler handler : HANDLERS) {
            if (handler.isActive(player)) {
                handler.onCoreKey(player);
                if (handler.blocksOthers()) return;
            }
        }
    }
}