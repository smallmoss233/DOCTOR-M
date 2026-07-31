package doctor_m.module.sonic_plus;

import dev.amble.ait.core.item.sonic.SonicMode;
import doctor_m.module.sonic_plus.ModeChargedZeiton.LaserMode;
import doctor_m.module.sonic_plus.ModeChargedZeiton.PulseMode;
import doctor_m.module.sonic_plus.ModeChargedZeiton.PushMode;

public class ChargedZeitonSonicModes {
    public static SonicMode map(SonicMode original) {
        return switch (original.index()) {
            case 0 -> PulseMode.INSTANCE;   // 原 INTERACTION
            case 1 -> LaserMode.INSTANCE;   // 原 OVERLOAD
            case 2 -> PushMode.INSTANCE;    // 原 SCANNING
            default -> original;
        };
    }
}