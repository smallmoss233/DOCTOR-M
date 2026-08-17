package doctor_m.mixin.client.title;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import doctor_m.client.util.id.GlowConditionChecker;
import doctor_m.client.util.id.GlowTextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
            )
    )
    private int doctor_m$glowChatLine(
            DrawContext context,
            TextRenderer textRenderer,
            OrderedText text,
            int x,
            int y,
            int color,
            Operation<Integer> original) {

        String content = orderedTextToString(text);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return original.call(context, textRenderer, text, x, y, color);

        for (PlayerEntity p : client.world.getPlayers()) {
            String name = p.getName().getString();
            int nameIdx = content.indexOf(name);

            if (nameIdx >= 0 && GlowConditionChecker.shouldGlow(p)) {
                // 同步点：改用 getPlayerTitleDirect
                String title = GlowConditionChecker.getPlayerTitleDirect(p);
                int nameWidth = textRenderer.getWidth(name);
                List<GlowTextRenderer.CharData> chars = GlowTextRenderer.orderedTextToList(text);
                int drawX = x;

                // 1. 前缀（保留原版颜色样式）
                if (nameIdx > 0) {
                    OrderedText prefix = GlowTextRenderer.subOrderedText(chars, 0, nameIdx);
                    drawX += context.drawTextWithShadow(textRenderer, prefix, drawX, y, color);
                }

                // 2. 名字/称号渐变（固定宽度=nameWidth，不会挤爆）
                GlowTextRenderer.draw2DAlternating(context, textRenderer, name, title, drawX, y, nameWidth, client.player);
                drawX += nameWidth;

                // 3. 后缀（保留原版颜色样式，位置固定）
                int nameEnd = nameIdx + name.length();
                if (nameEnd < chars.size()) {
                    OrderedText suffix = GlowTextRenderer.subOrderedText(chars, nameEnd, chars.size());
                    context.drawTextWithShadow(textRenderer, suffix, drawX, y, color);
                }

                return textRenderer.getWidth(text);
            }
        }

        return original.call(context, textRenderer, text, x, y, color);
    }

    private static String orderedTextToString(OrderedText text) {
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}