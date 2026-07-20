package doctor_m.util.tooltip;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.minecraft.text.Text;
import java.lang.reflect.Method;
import java.util.List;

public class ShiftTooltipInvoker {
    private static final String HELPER_CLASS = "doctor_m.util.ShiftTooltipHelper";
    private static final String METHOD_NAME = "addShiftTooltip";

    public static void addShiftTooltip(List<Text> tooltip, Text longText) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }

        try {
            Class<?> clazz = Class.forName(HELPER_CLASS);
            Method method = clazz.getDeclaredMethod(METHOD_NAME, List.class, Text.class);
            method.invoke(null, tooltip, longText);
        } catch (Exception e) {
            e.printStackTrace(); // 记录错误日志
        }
    }
}