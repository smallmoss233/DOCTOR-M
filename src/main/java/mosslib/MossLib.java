package mosslib;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MossLib {

    /** 你自己的命名空间。如果你的 mod id 不是 mosslib，改成 doctor_m。 */
    public static final String MOD_ID = "doctor_m";

    public static final Logger LOGGER = LoggerFactory.getLogger("MossLib");

    private MossLib() {}

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}