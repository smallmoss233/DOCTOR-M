package doctor_m.client;

import com.mojang.blaze3d.systems.RenderSystem;
import doctor_m.DOCTORM;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.Identifier;

public class ShieldOverlay implements HudRenderCallback {
    private static final Identifier TEXTURE = new Identifier(DOCTORM.MOD_ID, "textures/gui/shield_overlay.png");

    private static float alpha = 0f;
    private static int fadeTicks = 0;
    private static final int FADE_DURATION = 60; // 3秒淡出

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // 可选：只在第一人称显示（参考DelayOverlay）
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;

        // 更新淡出
        if (alpha > 0) {
            fadeTicks++;
            if (fadeTicks >= FADE_DURATION) {
                alpha = 0f;
            } else {
                alpha = 1.0f - (float) fadeTicks / FADE_DURATION;
            }
        }

        if (alpha < 0.01f) return;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // 关键修复：256x128 纹理要全屏拉伸，必须用矩阵缩放
        // 否则 UV 会超出 1.0，纹理会平铺 7~8 次
        context.getMatrices().push();
        context.getMatrices().scale((float) screenW / 256f, (float) screenH / 128f, 1.0f);
        context.drawTexture(TEXTURE, 0, 0, 0, 0, 256, 128, 256, 128);
        context.getMatrices().pop();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    public static void triggerShield() {
        alpha = 1.0f;
        fadeTicks = 0;
        System.out.println("[ShieldOverlay] 触发护盾叠加层");
    }

    public static void resetShield() {
        // 1秒内（20 ticks）再次受击则重置
        if (fadeTicks < 20) {
            alpha = 1.0f;
            fadeTicks = 0;
            System.out.println("[ShieldOverlay] 重置护盾叠加层");
        }
    }
}