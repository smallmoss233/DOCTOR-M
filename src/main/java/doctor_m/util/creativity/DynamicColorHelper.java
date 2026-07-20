package doctor_m.util.creativity;

import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.List;

public class DynamicColorHelper {

    /**
     * 在颜色列表之间循环渐变（支持最后一个颜色到第一个颜色的平滑过渡）
     * @param baseText 基础文本
     * @param colors 颜色列表（至少两个）
     * @param periodMs 完整周期（毫秒）
     */
    public static Text applyColorCycle(Text baseText, List<Color> colors, long periodMs) {
        if (colors == null || colors.size() < 2) {
            return baseText;
        }
        long time = System.currentTimeMillis();
        float t = (time % periodMs) / (float) periodMs;
        int segmentCount = colors.size(); // 颜色数量作为段数，循环回到第一个
        float segment = t * segmentCount;
        int index = (int) Math.floor(segment) % colors.size();
        int nextIndex = (index + 1) % colors.size();
        float localT = segment - (int) Math.floor(segment);
        Color c1 = colors.get(index);
        Color c2 = colors.get(nextIndex);
        int rgb = lerpColor(c1, c2, localT);
        return baseText.copy().styled(style -> style.withColor(TextColor.fromRgb(rgb)));
    }

    /**
     * 在颜色列表之间循环渐变，默认周期 8 秒
     */
    public static Text applyColorCycle(Text baseText, List<Color> colors) {
        return applyColorCycle(baseText, colors, 8000);
    }

    /**
     * 线性插值两个颜色
     */
    private static int lerpColor(Color a, Color b, float t) {
        int r = (int) MathHelper.lerp(t, a.getRed(), b.getRed());
        int g = (int) MathHelper.lerp(t, a.getGreen(), b.getGreen());
        int bComp = (int) MathHelper.lerp(t, a.getBlue(), b.getBlue());
        return (r << 16) | (g << 8) | bComp;
    }
}