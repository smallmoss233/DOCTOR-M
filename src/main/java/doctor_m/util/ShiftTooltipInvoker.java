package doctor_m.util;

import net.minecraft.text.Text;
import java.lang.reflect.Method;
import java.util.List;

public class ShiftTooltipInvoker {
    private static final String HELPER_CLASS = "doctor_m.util.ShiftTooltipHelper";
    private static final String METHOD_NAME = "addShiftTooltip";

    public static void addShiftTooltip(List<Text> tooltip, Text shortText, Text... detailedLines) {
        try {
            Class<?> clazz = Class.forName(HELPER_CLASS);
            Method method = clazz.getDeclaredMethod(METHOD_NAME, List.class, Text.class, Text[].class);
            method.invoke(null, tooltip, shortText, detailedLines);
        } catch (Exception e) {
            // 回退：仅显示简短描述
            tooltip.add(shortText);
        }
    }
}