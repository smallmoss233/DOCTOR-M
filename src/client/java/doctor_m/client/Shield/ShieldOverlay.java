package doctor_m.client.Shield;

import com.mojang.blaze3d.systems.RenderSystem;
import doctor_m.DOCTORM;
import doctor_m.api.ModSounds;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;

public class ShieldOverlay implements HudRenderCallback {
    private static final Identifier TEXTURE = new Identifier(DOCTORM.MOD_ID, "textures/gui/shield_overlay.png");

    private static float alpha = 0f;
    private static int fadeTicks = 0;
    private static final int FADE_DURATION = 60;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;

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
        playShieldSound();
    }

    public static void resetShield() {
        if (fadeTicks < 20) {
            alpha = 1.0f;
            fadeTicks = 0;
            playShieldSound();
        }
    }

    private static void playShieldSound() {
        float pitch = 0.9f + (float) Math.random() * 0.2f; // 0.9 ~ 1.1
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(ModSounds.SHIELD_ACTIVATE, pitch, 1.0f)
        );
    }
}