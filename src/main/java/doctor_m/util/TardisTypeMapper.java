package doctor_m.util;

import net.minecraft.util.Identifier;

public class TardisTypeMapper {
    public static String getTypeForDesktop(Identifier desktopId) {
        return TardisTypeLoader.getTypeForDesktop(desktopId);
    }
}