package doctor_m.module.sonic_plus;

import dev.amble.ait.core.item.sonic.SonicMode;
import doctor_m.module.sonic_plus.ModeGravitational.GravitationalDragMode;
import doctor_m.module.sonic_plus.ModeGravitational.GravitationalShieldingMode;
import doctor_m.module.sonic_plus.ModeGravitational.GravitationalSwapMode;

public class AmethystSonicModes {
    public static SonicMode map(SonicMode original) {
        return switch (original.index()) {
            case 0 -> GravitationalDragMode.INSTANCE;        // ← 原 INTERACTION
            case 1 -> GravitationalShieldingMode.INSTANCE;     // ← 原 OVERLOAD
            case 2 -> GravitationalSwapMode.INSTANCE;          // ← 原 SCANNING
            default -> original;                               // TARDIS (3) 不动
        };
    }
}