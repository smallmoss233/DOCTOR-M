package doctor_m.client.util.id;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class GlowTextRenderer {

    public static final int COLOR_BRIGHT  = 0x55FF55;
    public static final int COLOR_DARK    = 0x006600;
    public static final int COLOR_HALO    = 0x6644FF44;
    public static final int COLOR_RED_BRIGHT = 0xFF5555;   // 亮红
    public static final int COLOR_RED_DARK   = 0x660000;   // 暗红
    public static final int COLOR_HALO_RED   = 0x66FF4444; // 红色光晕（半透明红）

    // 总周期 8 秒
    public static final long CYCLE_MS = 8000L;
    // 名字显示时长（不含淡出）
    public static final long NAME_SHOW_MS = 3000L;
    // 过渡时长（淡出/淡入）
    public static final long FADE_MS = 500L;
    // 称号等待时长（显示但不滚动）
    public static final long TITLE_WAIT_MS = 1000L;

    // 时间点常量（单位 ms）
    private static final long NAME_END = NAME_SHOW_MS;                                    // 3000
    private static final long FADE1_END = NAME_END + FADE_MS;                            // 3500
    private static final long SCROLL_START = FADE1_END + TITLE_WAIT_MS;                  // 4500
    private static final long FADE2_START = CYCLE_MS - FADE_MS;                          // 7500
    private static final long FADE2_END = CYCLE_MS;                                      // 8000

    private GlowTextRenderer() {}

    // ==================== Alpha 计算 ====================

    public static float getNameAlpha() {
        long t = System.currentTimeMillis() % CYCLE_MS;
        if (t <= NAME_END) return 1.0f;
        if (t <= FADE1_END) {
            return 1.0f - (t - NAME_END) / (float) FADE_MS;
        }
        if (t <= FADE2_START) return 0.0f;
        if (t <= FADE2_END) {
            return (t - FADE2_START) / (float) FADE_MS;
        }
        return 1.0f;
    }

    public static float getTitleAlpha() {
        long t = System.currentTimeMillis() % CYCLE_MS;
        if (t <= NAME_END) return 0.0f;
        if (t <= FADE1_END) {
            return (t - NAME_END) / (float) FADE_MS;
        }
        if (t <= FADE2_START) return 1.0f;
        if (t <= FADE2_END) {
            return 1.0f - (t - FADE2_START) / (float) FADE_MS;
        }
        return 0.0f;
    }

    // ==================== 呼吸颜色 ====================

    public static int breatheColorWithAlpha(int bright, int dark, long periodMs, float alpha) {
        float t = (System.currentTimeMillis() % periodMs) / (float) periodMs;
        float breathe = (MathHelper.sin(t * MathHelper.TAU) + 1f) / 2f;

        int r = (int) MathHelper.lerp(breathe, (bright >> 16) & 0xFF, (dark >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(breathe, (bright >> 8)  & 0xFF, (dark >> 8)  & 0xFF);
        int b = (int) MathHelper.lerp(breathe, bright & 0xFF,         dark & 0xFF);
        int a = MathHelper.clamp((int) (alpha * 255), 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ==================== OrderedText 工具 ====================

    public record CharData(int codePoint, Style style) {}

    public static List<CharData> orderedTextToList(OrderedText text) {
        List<CharData> list = new ArrayList<>();
        text.accept((index, style, codePoint) -> {
            list.add(new CharData(codePoint, style));
            return true;
        });
        return list;
    }

    public static OrderedText subOrderedText(List<CharData> chars, int start, int end) {
        return visitor -> {
            for (int i = start; i < end && i < chars.size(); i++) {
                CharData cd = chars.get(i);
                if (!visitor.accept(i, cd.style, cd.codePoint)) return false;
            }
            return true;
        };
    }

    // ==================== 滚动截取（动态进度，保证淡出前完成） ====================

    private static String getScrollingSubstring(String fullTitle, int maxWidth, TextRenderer renderer, long cycleTime) {
        if (fullTitle == null || fullTitle.isEmpty()) return fullTitle;
        int totalWidth = renderer.getWidth(fullTitle);
        if (totalWidth <= maxWidth) return fullTitle;

        int maxOffset = totalWidth - maxWidth;

        // 等待阶段：显示开头
        if (cycleTime < SCROLL_START) {
            return truncateToWidth(fullTitle, maxWidth, renderer);
        }

        // 滚动阶段：从 SCROLL_START 到 FADE2_START 均匀推进
        long scrollDuration = (long)((FADE2_START - SCROLL_START) * 0.8);
        long elapsed = Math.min(cycleTime - SCROLL_START, scrollDuration);
        float progress = Math.min(1.0f, (float) elapsed / scrollDuration);
        int offset = (int) (progress * maxOffset);
        if (offset > maxOffset) offset = maxOffset;

        // 如果到达末尾，从末尾取
        if (offset >= maxOffset) {
            return substringFromEnd(fullTitle, maxWidth, renderer);
        } else {
            return substringByPixelOffset(fullTitle, offset, maxWidth, renderer);
        }
    }

    /**
     * 根据像素偏移截取子串，保证显示宽度不超过 maxWidth
     */
    private static String substringByPixelOffset(String full, int offsetPx, int maxWidth, TextRenderer renderer) {
        if (offsetPx <= 0) {
            return truncateToWidth(full, maxWidth, renderer);
        }

        // 1. 计算起始字符索引
        int startIndex = 0;
        int cum = 0;
        for (int i = 0; i < full.length(); i++) {
            String ch = String.valueOf(full.charAt(i));
            int cw = renderer.getWidth(ch);
            if (cum + cw > offsetPx) {
                startIndex = i;
                break;
            }
            cum += cw;
        }

        // 2. 检查从起始索引到末尾的宽度是否足够
        int remainingWidth = renderer.getWidth(full.substring(startIndex));
        if (remainingWidth <= maxWidth) {
            // 剩余宽度不足，直接从末尾取，确保完整显示末尾
            return substringFromEnd(full, maxWidth, renderer);
        }

        // 3. 正常截取
        StringBuilder result = new StringBuilder();
        int currentWidth = 0;
        for (int i = startIndex; i < full.length(); i++) {
            String ch = String.valueOf(full.charAt(i));
            int cw = renderer.getWidth(ch);
            if (currentWidth + cw > maxWidth) break;
            result.append(ch);
            currentWidth += cw;
        }
        return result.toString();
    }

    /**
     * 从字符串末尾向前截取，保证末尾字符完整
     */
    private static String substringFromEnd(String full, int maxWidth, TextRenderer renderer) {
        int totalWidth = renderer.getWidth(full);
        if (totalWidth <= maxWidth) return full;
        StringBuilder sb = new StringBuilder();
        int currentWidth = 0;
        for (int i = full.length() - 1; i >= 0; i--) {
            String ch = String.valueOf(full.charAt(i));
            int cw = renderer.getWidth(ch);
            if (currentWidth + cw <= maxWidth) {
                sb.insert(0, ch);
                currentWidth += cw;
            } else {
                if (currentWidth == 0 && cw <= maxWidth) {
                    sb.insert(0, ch);
                }
                break;
            }
        }
        return sb.toString();
    }

    private static String truncateToWidth(String text, int maxWidth, TextRenderer renderer) {
        if (renderer.getWidth(text) <= maxWidth) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String test = sb.toString() + text.charAt(i);
            if (renderer.getWidth(test) > maxWidth) break;
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    // ==================== 2D 绘制 ====================
    public static void draw2DAlternating(DrawContext context, TextRenderer renderer,
                                         String name, String originalTitle, int x, int y,
                                         int fixedWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        int glowColor = GlowConditionChecker.getGlowColorByName(name, client);
        String title = GlowConditionChecker.getGlowTitleByName(name, client);
        if (title == null) title = originalTitle;

        // 根据颜色选择暗色和光晕
        boolean isRed = (glowColor == GlowConditionChecker.COLOR_RED);
        int darkColor = isRed ? COLOR_RED_DARK : COLOR_DARK;
        int haloColorBase = isRed ? COLOR_HALO_RED : COLOR_HALO;

        // 空称号分支
        if (title == null || title.isEmpty()) {
            int color = breatheColorWithAlpha(glowColor != 0 ? glowColor : COLOR_BRIGHT, darkColor, 3000L, 1.0f);
            int halo = withAlpha(haloColorBase, 0.4f);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    context.drawText(renderer, name, x + dx, y + dy, halo, false);
                }
            }
            context.drawText(renderer, name, x, y, color, true);
            return;
        }

        float nameAlpha = getNameAlpha();
        float titleAlpha = getTitleAlpha();
        long cycleTime = System.currentTimeMillis() % CYCLE_MS;

        // ---- 名字 ----
        if (nameAlpha > 0.01f) {
            int halo = withAlpha(haloColorBase, nameAlpha * 0.4f);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    context.drawText(renderer, name, x + dx, y + dy, halo, false);
                }
            }
            int mainColor = breatheColorWithAlpha(glowColor != 0 ? glowColor : COLOR_BRIGHT, darkColor, 3000L, nameAlpha);
            context.drawText(renderer, name, x, y, mainColor, true);
        }

        // ---- 称号 ----
        if (titleAlpha > 0.01f) {
            String visibleTitle = getScrollingSubstring(title, fixedWidth, renderer, cycleTime);
            if (visibleTitle != null && !visibleTitle.isEmpty()) {
                int halo = withAlpha(haloColorBase, titleAlpha * 0.4f);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        context.drawText(renderer, visibleTitle, x + dx, y + dy, halo, false);
                    }
                }
                int mainColor = breatheColorWithAlpha(glowColor != 0 ? glowColor : COLOR_BRIGHT, darkColor, 3000L, titleAlpha);
                context.drawText(renderer, visibleTitle, x, y, mainColor, true);
            }
        }
    }

    // ==================== 3D 绘制 ====================
    public static int draw3DAlternating(TextRenderer renderer, String name, String originalTitle,
                                        float x, float y, boolean shadow,
                                        Matrix4f matrix, VertexConsumerProvider vertexConsumers,
                                        TextRenderer.TextLayerType layerType,
                                        int backgroundColor, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        int glowColor = GlowConditionChecker.getGlowColorByName(name, client);
        String title = GlowConditionChecker.getGlowTitleByName(name, client);
        if (title == null) title = originalTitle;

        int nameWidth = renderer.getWidth(name);
        boolean isRed = (glowColor == GlowConditionChecker.COLOR_RED);
        int darkColor = isRed ? COLOR_RED_DARK : COLOR_DARK;
        int haloColorBase = isRed ? COLOR_HALO_RED : COLOR_HALO;

        // 空称号
        if (title == null || title.isEmpty()) {
            int color = breatheColorWithAlpha(glowColor != 0 ? glowColor : COLOR_BRIGHT, darkColor, 3000L, 1.0f);
            int halo = withAlpha(haloColorBase, 0.4f);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    renderer.draw(Text.literal(name), x + dx * 0.4f, y + dy * 0.4f,
                            halo, false, matrix, vertexConsumers,
                            TextRenderer.TextLayerType.SEE_THROUGH, 0, light);
                }
            }
            renderer.draw(Text.literal(name), x, y, color, shadow,
                    matrix, vertexConsumers, layerType, backgroundColor, light);
            return (int) (nameWidth * 0.025f);
        }

        float nameAlpha = getNameAlpha();
        float titleAlpha = getTitleAlpha();
        long cycleTime = System.currentTimeMillis() % CYCLE_MS;

        // 名字
        if (nameAlpha > 0.01f) {
            int halo = withAlpha(haloColorBase, nameAlpha * 0.4f);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    renderer.draw(Text.literal(name), x + dx * 0.4f, y + dy * 0.4f,
                            halo, false, matrix, vertexConsumers,
                            TextRenderer.TextLayerType.SEE_THROUGH, 0, light);
                }
            }
            int main = breatheColorWithAlpha(glowColor != 0 ? glowColor : COLOR_BRIGHT, darkColor, 3000L, nameAlpha);
            renderer.draw(Text.literal(name), x, y, main, shadow,
                    matrix, vertexConsumers, layerType, backgroundColor, light);
        }

        // 称号
        if (titleAlpha > 0.01f) {
            String visibleTitle = getScrollingSubstring(title, nameWidth, renderer, cycleTime);
            if (visibleTitle != null && !visibleTitle.isEmpty()) {
                int halo = withAlpha(haloColorBase, titleAlpha * 0.4f);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        renderer.draw(Text.literal(visibleTitle), x + dx * 0.4f, y + dy * 0.4f,
                                halo, false, matrix, vertexConsumers,
                                TextRenderer.TextLayerType.SEE_THROUGH, 0, light);
                    }
                }
                int main = breatheColorWithAlpha(glowColor != 0 ? glowColor : COLOR_BRIGHT, darkColor, 3000L, titleAlpha);
                renderer.draw(Text.literal(visibleTitle), x, y, main, shadow,
                        matrix, vertexConsumers, layerType, backgroundColor, light);
            }
        }

        return (int) (nameWidth * 0.025f);
    }

    // ==================== 辅助方法 ====================
    private static int withAlpha(int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}