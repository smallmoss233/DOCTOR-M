package doctor_m.util;

import net.minecraft.text.Text;
import java.util.List;

public class TooltipHelper {
    public static void addWrappedTooltip(List<Text> tooltip, Text originalText) {
        String raw = originalText.getString();
        String[] lines = raw.split("\\*");
        for (String line : lines) {
            if (!line.isEmpty()) {
                tooltip.add(Text.literal(line));
            }
        }
    }
}