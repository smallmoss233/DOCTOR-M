package doctor_m.util;

import net.minecraft.text.Text;
import java.util.List;

public class TooltipHelper {
    public static void addWrappedTooltip(List<Text> tooltip, Text originalText) {
        String raw = originalText.getString();
        // 提取第一个颜色代码（例如 §7, §c 等）
        String colorCode = "";
        int index = raw.indexOf('§');
        if (index != -1 && index + 1 < raw.length()) {
            char code = raw.charAt(index + 1);
            colorCode = "§" + code;
        }
        String[] lines = raw.split("\\*");
        for (String line : lines) {
            if (!line.isEmpty()) {
                // 为每行添加相同的颜色代码（如果存在）
                String coloredLine = colorCode + line;
                tooltip.add(Text.literal(coloredLine));
            }
        }
    }
}