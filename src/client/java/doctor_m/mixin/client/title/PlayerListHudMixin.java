package doctor_m.mixin.client.title;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import doctor_m.client.util.id.GlowConditionChecker;
import doctor_m.client.util.id.GlowTextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
            )
    )
    private int doctor_m$glowTabName(
            DrawContext context,
            TextRenderer textRenderer,
            OrderedText text,
            int x,
            int y,
            int color,
            Operation<Integer> original) {

        String raw = orderedTextToString(text);
        MinecraftClient client = MinecraftClient.getInstance();

        if (GlowConditionChecker.shouldGlowByName(raw, client)) {
            String pureName = stripFormatting(raw);
            String title = GlowConditionChecker.getTitleByName(raw, client);
            int nameWidth = textRenderer.getWidth(pureName);

            PlayerEntity targetPlayer = null;
            if (client.world != null) {
                targetPlayer = client.world.getPlayers().stream()
                        .filter(p -> stripFormatting(p.getName().getString()).equals(pureName))
                        .findFirst()
                        .orElse(null);
            }

            GlowTextRenderer.draw2DAlternating(context, textRenderer, pureName, title, x, y, nameWidth, targetPlayer);
            return textRenderer.getWidth(text);
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

    private static String stripFormatting(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }
}