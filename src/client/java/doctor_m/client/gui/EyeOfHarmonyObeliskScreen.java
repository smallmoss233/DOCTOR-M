package doctor_m.client.gui;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import doctor_m.network.UpdateObeliskPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class EyeOfHarmonyObeliskScreen extends Screen {

    private final EyeOfHarmonyObeliskBlockEntity blockEntity;
    private int currentYOffset;
    private float currentScale;
    private boolean currentEyeVisible;
    private boolean currentRedstoneMode;

    private static final int MIN_Y = -100;
    private static final int MAX_Y = 100;
    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 5.0f;

    private static final int COLOR_OVERLAY      = 0xE6000000;
    private static final int COLOR_PANEL        = 0xFF0D1117;
    private static final int COLOR_PANEL_BORDER = 0xFF30363D;
    private static final int COLOR_ACCENT       = 0xFF00D4AA;
    private static final int COLOR_TEXT_PRIMARY = 0xFFE6EDF3;
    private static final int COLOR_DIVIDER      = 0xFF21262D;
    private static final int COLOR_CARD_BG      = 0xFF161B22;
    private static final int COLOR_CARD_BORDER  = 0xFF30363D;
    private static final int COLOR_TRACK_BG     = 0xFF0D1117;
    private static final int COLOR_BTN_BG       = 0xFF21262D;
    private static final int COLOR_BTN_HOVER    = 0xFF30363D;

    private static final int PANEL_WIDTH  = 300;
    private static final int PANEL_HEIGHT = 340; // 增高

    private int panelX, panelY;

    public EyeOfHarmonyObeliskScreen(EyeOfHarmonyObeliskBlockEntity blockEntity) {
        super(Text.translatable("gui.doctor_m.obelisk.title"));
        this.blockEntity = blockEntity;
        this.currentYOffset = Math.round(blockEntity.getYOffset());
        this.currentScale = blockEntity.getScale();
        this.currentEyeVisible = blockEntity.isEyeVisible();
        this.currentRedstoneMode = blockEntity.isRedstoneMode();
    }

    @Override
    protected void init() {
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;

        int cx = panelX + 24;
        int cy = panelY + 110; // 整体下移，和能量卡片拉开距离

        // Y轴偏移
        double yVal = (double) (currentYOffset - MIN_Y) / (MAX_Y - MIN_Y);
        addDrawableChild(new ModernSlider(
                cx, cy, 252, 20,
                Text.translatable("gui.doctor_m.obelisk.y_offset", currentYOffset), yVal
        ) {
            @Override protected void updateMessage() {
                int v = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                setMessage(Text.translatable("gui.doctor_m.obelisk.y_offset", v));
            }
            @Override protected void applyValue() {
                currentYOffset = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                blockEntity.setYOffset(currentYOffset);
                syncToServer();
            }
        });

        // 缩放（间距从36→42）
        double sVal = (double) (currentScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        addDrawableChild(new ModernSlider(
                cx, cy + 42, 252, 20,
                Text.translatable("gui.doctor_m.obelisk.scale", String.format("%.1f", currentScale)), sVal
        ) {
            @Override protected void updateMessage() {
                float v = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                v = Math.round(v * 10.0f) / 10.0f;
                setMessage(Text.translatable("gui.doctor_m.obelisk.scale", String.format("%.1f", v)));
            }
            @Override protected void applyValue() {
                currentScale = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                currentScale = Math.round(currentScale * 10.0f) / 10.0f;
                blockEntity.setScale(currentScale);
                syncToServer();
            }
        });

        // 复选框（间距拉大）
        addDrawableChild(new ModernCheck(
                cx, cy + 84, 252, 20,
                Text.translatable("gui.doctor_m.obelisk.render_eye"), currentEyeVisible
        ) {
            @Override public void onPress() {
                super.onPress();
                currentEyeVisible = this.isChecked();
                blockEntity.setEyeVisible(currentEyeVisible);
                syncToServer();
            }
        });

        addDrawableChild(new ModernCheck(
                cx, cy + 112, 252, 20,
                Text.translatable("gui.doctor_m.obelisk.redstone_control"), currentRedstoneMode
        ) {
            @Override public void onPress() {
                super.onPress();
                currentRedstoneMode = this.isChecked();
                blockEntity.setRedstoneMode(currentRedstoneMode);
                syncToServer();
            }
        });

        // 完成按钮
        addDrawableChild(new ModernButton(
                panelX + PANEL_WIDTH / 2 - 50, panelY + PANEL_HEIGHT - 36, 100, 20,
                Text.translatable("gui.doctor_m.obelisk.done"),
                b -> this.close()
        ));
    }

    private void syncToServer() {
        ClientPlayNetworking.send(
                UpdateObeliskPacket.ID,
                UpdateObeliskPacket.createBuf(
                        blockEntity.getPos(),
                        (float) currentYOffset,
                        currentScale,
                        currentEyeVisible,
                        currentRedstoneMode
                )
        );
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = panelX + 24;

        ctx.fill(0, 0, this.width, this.height, COLOR_OVERLAY);

        ctx.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, 0xFF080808);
        ctx.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL);
        ctx.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, COLOR_PANEL_BORDER);
        ctx.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 3, COLOR_ACCENT);

        // 标题
        String title = Text.translatable("gui.doctor_m.obelisk.title").getString();
        int tw = this.textRenderer.getWidth(title);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(title).formatted(Formatting.WHITE),
                panelX + (PANEL_WIDTH - tw) / 2, panelY + 14, COLOR_TEXT_PRIMARY);

        // 标题下分隔线
        int lineY = panelY + 30;
        ctx.fill(panelX + 20, lineY, panelX + PANEL_WIDTH - 20, lineY + 1, COLOR_DIVIDER);
        int dot = panelX + (PANEL_WIDTH - 4) / 2;
        ctx.fill(dot, lineY, dot + 4, lineY + 1, COLOR_ACCENT);

        // ====== 能量卡片 ======
        int cardX = panelX + 16;
        int cardY = panelY + 38;
        int cardW = PANEL_WIDTH - 32;
        int cardH = 54;

        ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_CARD_BG);
        ctx.drawBorder(cardX, cardY, cardW, cardH, COLOR_CARD_BORDER);

        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("ENERGY").formatted(Formatting.GRAY),
                cardX + 10, cardY + 7, 0xFF6E7681);

        int barX = cardX + 10, barY = cardY + 22, barW = cardW - 20, barH = 8;
        double pct = blockEntity.getEnergyPercentage();
        int fill = (int) (barW * pct);

        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0C10);
        ctx.drawBorder(barX, barY, barW, barH, 0xFF1C2128);
        if (fill > 0) {
            int c = energyColor(pct);
            ctx.fill(barX + 1, barY + 1, barX + fill - 1, barY + barH - 1, c);
            ctx.fill(barX + 1, barY + 1, barX + fill - 1, barY + 3, c | 0xFF444444);
        }

        String energyText = String.format("%.0f / %.0f AU  (%.0f%%)",
                blockEntity.getCurrentFuel(),
                blockEntity.getMaxFuel(),
                pct * 100);
        int tx = cardX + (cardW - textRenderer.getWidth(energyText)) / 2;
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(energyText).formatted(Formatting.WHITE),
                tx, barY + 12, COLOR_TEXT_PRIMARY);

        // ====== 状态区 ======
        int statusY = panelY + PANEL_HEIGHT - 58;
        ctx.fill(cx, statusY - 10, cx + 252, statusY - 9, COLOR_DIVIDER);

        boolean active = blockEntity.isActive();
        int dotColor = active ? 0xFF00D4AA : 0xFFFF5555;
        int dotX = panelX + (PANEL_WIDTH - 8) / 2 - 40;

        ctx.fill(dotX, statusY, dotX + 6, statusY + 6, dotColor);
        ctx.fill(dotX - 1, statusY - 1, dotX + 7, statusY + 7, dotColor & 0x40FFFFFF);

        String statusKey = active
                ? "gui.doctor_m.obelisk.active"
                : "gui.doctor_m.obelisk.inactive";
        Text statusText = Text.translatable("gui.doctor_m.obelisk.status",
                Text.translatable(statusKey).formatted(active ? Formatting.GREEN : Formatting.RED));
        int sw = this.textRenderer.getWidth(statusText);
        ctx.drawTextWithShadow(this.textRenderer, statusText,
                panelX + (PANEL_WIDTH - sw) / 2 + 8, statusY + 1, COLOR_TEXT_PRIMARY);

        super.render(ctx, mx, my, delta);
    }

    private int energyColor(double pct) {
        int r, g;
        if (pct < 0.5) {
            r = 0xFF;
            g = (int) (0xFF * (pct * 2));
        } else {
            r = (int) (0xFF * (1 - (pct - 0.5) * 2));
            g = 0xFF;
        }
        if (pct > 0.8) {
            r = (int) (r * 0.3);
            g = 0xFF;
        }
        return 0xFF000000 | (r << 16) | (g << 8) | 0x00;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ========== 自定义现代滑块（手柄改小） ==========
    private abstract class ModernSlider extends SliderWidget {
        ModernSlider(int x, int y, int w, int h, Text msg, double val) {
            super(x, y, w, h, msg, val);
        }

        @Override
        public void renderButton(DrawContext ctx, int mx, int my, float delta) {
            int tx = this.getX();
            int ty = this.getY();
            int tw = this.getWidth();

            ctx.drawTextWithShadow(textRenderer, getMessage(), tx, ty - 10, COLOR_TEXT_PRIMARY);

            int trackY = ty + 6;
            int trackH = 4;

            ctx.fill(tx, trackY, tx + tw, trackY + trackH, COLOR_TRACK_BG);
            ctx.drawBorder(tx, trackY, tw, trackH, 0xFF1C2128);

            int fillW = (int) (tw * this.value);
            if (fillW > 0) {
                ctx.fill(tx + 1, trackY + 1, tx + fillW - 1, trackY + trackH - 1, COLOR_ACCENT);
                ctx.fill(tx + 1, trackY + 1, tx + fillW - 1, trackY + 2, 0xFF4DFFB8);
            }

            // 手柄改小：8x12
            int handleW = 8;
            int handleH = 12;
            int hx = tx + fillW - handleW / 2;
            hx = MathHelper.clamp(hx, tx - 2, tx + tw - handleW + 2);
            int hy = trackY - (handleH - trackH) / 2;

            boolean hovered = isHovered();
            int hc = hovered ? 0xFF00F0C8 : COLOR_ACCENT;
            ctx.fill(hx, hy, hx + handleW, hy + handleH, hc);
            ctx.drawBorder(hx, hy, handleW, handleH, hovered ? 0xFFFFFFFF : 0xFF00A080);
            ctx.fill(hx + 3, hy + 3, hx + 5, hy + handleH - 3, 0xFF0D1117);
        }
    }

    // ========== 自定义现代复选框 ==========
    private abstract class ModernCheck extends CheckboxWidget {
        ModernCheck(int x, int y, int w, int h, Text msg, boolean checked) {
            super(x, y, w, h, msg, checked);
        }

        @Override
        public void renderButton(DrawContext ctx, int mx, int my, float delta) {
            int tx = this.getX();
            int ty = this.getY();
            int box = 12;

            ctx.fill(tx, ty, tx + box, ty + box, COLOR_CARD_BG);
            boolean hovered = isHovered();
            ctx.drawBorder(tx, ty, box, box, hovered ? COLOR_ACCENT : COLOR_CARD_BORDER);

            if (isChecked()) {
                ctx.fill(tx + 2, ty + 2, tx + box - 2, ty + box - 2, COLOR_ACCENT);
                ctx.fill(tx + 3, ty + 6, tx + 5, ty + 8, 0xFF0D1117);
                ctx.fill(tx + 5, ty + 4, tx + 7, ty + 6, 0xFF0D1117);
                ctx.fill(tx + 7, ty + 2, tx + 9, ty + 4, 0xFF0D1117);
            }

            ctx.drawTextWithShadow(textRenderer, getMessage(), tx + box + 6, ty + 2, COLOR_TEXT_PRIMARY);
        }
    }

    // ========== 自定义现代按钮 ==========
    private class ModernButton extends ButtonWidget {
        ModernButton(int x, int y, int w, int h, Text msg, PressAction action) {
            super(x, y, w, h, msg, action, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        public void renderButton(DrawContext ctx, int mx, int my, float delta) {
            int tx = this.getX();
            int ty = this.getY();
            int tw = this.getWidth();
            int th = this.getHeight();

            boolean hovered = isHovered();
            int bg = hovered ? COLOR_BTN_HOVER : COLOR_BTN_BG;
            ctx.fill(tx, ty, tx + tw, ty + th, bg);
            ctx.drawBorder(tx, ty, tw, th, hovered ? COLOR_ACCENT : 0xFF30363D);

            if (hovered) {
                ctx.fill(tx + 1, ty + 1, tx + tw - 1, ty + 2, 0xFF00D4AA);
            }

            int color = this.active ? (hovered ? COLOR_ACCENT : COLOR_TEXT_PRIMARY) : 0xFF555555;
            Text msg = getMessage();
            int mw = MinecraftClient.getInstance().textRenderer.getWidth(msg);
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    msg, tx + (tw - mw) / 2, ty + (th - 8) / 2, color);
        }
    }
}