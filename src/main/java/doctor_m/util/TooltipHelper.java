package doctor_m.util;

import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import java.util.List;

public class TooltipHelper {

    /**
     * 将长文本按最大字符数自动拆分成多行 Tooltip。
     * @param tooltip 原有的 tooltip 列表
     * @param originalText 原始文本（支持 Text 对象）
     * @param maxChars 每行最大字符数（建议 25~35）
     */
    public static void addWrappedTooltip(List<Text> tooltip, Text originalText, int maxChars) {
        String raw = originalText.getString();
        if (raw.length() <= maxChars) {
            tooltip.add(originalText);
            return;
        }

        StringBuilder line = new StringBuilder();
        MutableText currentLine = Text.empty();
        for (char c : raw.toCharArray()) {
            line.append(c);
            currentLine = Text.literal(line.toString());
            if (line.length() >= maxChars && c == ' ') {
                tooltip.add(currentLine);
                line.setLength(0);
                currentLine = Text.empty();
            }
        }
        if (line.length() > 0) {
            tooltip.add(currentLine);
        }
    }
}