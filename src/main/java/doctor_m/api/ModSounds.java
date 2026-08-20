package doctor_m.api;

import doctor_m.DOCTORM;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent TOYOTA_TICKING_LOOP = register("toyota_ticking_loop");
    public static final SoundEvent TOYOTA_TICKING_START = register("toyota_ticking_start");
    public static final SoundEvent TOYOTA_TICKING_STOP = register("toyota_ticking_stop");
    public static final SoundEvent DE_MAT_GUN_FIRE = register("item.de_mat_gun.fire");
    public static final SoundEvent DE_MAT_GUN_ERASE = register("entity.de_mat_gun.erase");
    public static final SoundEvent SHIELD_ACTIVATE = register("shieldcore");

    private static SoundEvent register(String name) {
        Identifier id = new Identifier(DOCTORM.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {}
}