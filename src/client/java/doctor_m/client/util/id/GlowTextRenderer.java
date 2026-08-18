package doctor_m.client.util.id;

import doctor_m.Item.data_itme.KeytoTimeItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class GlowTextRenderer {

    public static final int COLOR_BRIGHT  = 0x55FF55;
    public static final int COLOR_DARK    = 0x006600;
    public static final int COLOR_HALO    = 0x6644FF44;
    public static final int COLOR_RED_DARK   = 0x660000;
    public static final int COLOR_HALO_RED   = 0x66FF4444;

    public static final long CYCLE_MS = 8000L;
    public static final long NAME_SHOW_MS = 3000L;
    public static final long FADE_MS = 500L;
    public static final long TITLE_WAIT_MS = 1000L;

    private static final long NAME_END = NAME_SHOW_MS;
    private static final long FADE1_END = NAME_END + FADE_MS;
    private static final long SCROLL_START = FADE1_END + TITLE_WAIT_MS;
    private static final long FADE2_START = CYCLE_MS - FADE_MS;
    private static final long FADE2_END = CYCLE_MS;

    private GlowTextRenderer() {}

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

    public static int breatheColorWithAlpha(int bright, int dark, long periodMs, float alpha) {
        float t = (System.currentTimeMillis() % periodMs) / (float) periodMs;
        float breathe = (MathHelper.sin(t * MathHelper.TAU) + 1f) / 2f;

        int r = (int) MathHelper.lerp(breathe, (bright >> 16) & 0xFF, (dark >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(breathe, (bright >> 8)  & 0xFF, (dark >> 8)  & 0xFF);
        int b = (int) MathHelper.lerp(breathe, bright & 0xFF,         dark & 0xFF);
        int a = MathHelper.clamp((int) (alpha * 255), 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

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

    private static String getScrollingSubstring(String fullTitle, int maxWidth, TextRenderer renderer, long cycleTime) {
        if (fullTitle == null || fullTitle.isEmpty()) return fullTitle;
        int totalWidth = renderer.getWidth(fullTitle);
        if (totalWidth <= maxWidth) return fullTitle;

        int maxOffset = totalWidth - maxWidth;

        if (cycleTime < SCROLL_START) {
            return truncateToWidth(fullTitle, maxWidth, renderer);
        }

        long scrollDuration = (long)((FADE2_START - SCROLL_START) * 0.8);
        long elapsed = Math.min(cycleTime - SCROLL_START, scrollDuration);
        float progress = Math.min(1.0f, (float) elapsed / scrollDuration);
        int offset = (int) (progress * maxOffset);
        if (offset > maxOffset) offset = maxOffset;

        if (offset >= maxOffset) {
            return substringFromEnd(fullTitle, maxWidth, renderer);
        } else {
            return substringByPixelOffset(fullTitle, offset, maxWidth, renderer);
        }
    }

    private static String substringByPixelOffset(String full, int offsetPx, int maxWidth, TextRenderer renderer) {
        if (offsetPx <= 0) {
            return truncateToWidth(full, maxWidth, renderer);
        }

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

        int remainingWidth = renderer.getWidth(full.substring(startIndex));
        if (remainingWidth <= maxWidth) {
            return substringFromEnd(full, maxWidth, renderer);
        }

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

    public static void draw2DAlternating(DrawContext context, TextRenderer renderer,
                                         String name, String originalTitle, int x, int y,
                                         int fixedWidth, @Nullable PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        int glowColor = GlowConditionChecker.getGlowColorByName(name, client);
        String title = GlowConditionChecker.getGlowTitleByName(name, client);
        if (title == null) {
            title = KeytoTimeItem.getTitleFromPlayer(player);
        }
        if (title == null) title = originalTitle;

        boolean isRed = (glowColor == GlowConditionChecker.COLOR_RED);
        int darkColor = isRed ? COLOR_RED_DARK : COLOR_DARK;
        int haloColorBase = isRed ? COLOR_HALO_RED : COLOR_HALO;

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

    public static int draw3DAlternating(TextRenderer renderer, String name, String originalTitle,
                                        float x, float y, boolean shadow,
                                        Matrix4f matrix, VertexConsumerProvider vertexConsumers,
                                        TextRenderer.TextLayerType layerType,
                                        int backgroundColor, int light,
                                        @Nullable PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        int glowColor = GlowConditionChecker.getGlowColorByName(name, client);
        String title = GlowConditionChecker.getGlowTitleByName(name, client);
        if (title == null) {
            title = KeytoTimeItem.getTitleFromPlayer(player);
        }
        if (title == null) title = originalTitle;

        int nameWidth = renderer.getWidth(name);
        boolean isRed = (glowColor == GlowConditionChecker.COLOR_RED);
        int darkColor = isRed ? COLOR_RED_DARK : COLOR_DARK;
        int haloColorBase = isRed ? COLOR_HALO_RED : COLOR_HALO;

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

    private static int withAlpha(int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}