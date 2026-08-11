package doctor_m.client.gui;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import doctor_m.network.UpdateObeliskPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    // ========== Time Lord 古老贵族风配色 ==========
    private static final int COLOR_OVERLAY       = 0xE6000000;
    private static final int COLOR_PANEL         = 0xFF1A0A0E; // 深酒红
    private static final int COLOR_PANEL_BORDER  = 0xFF8B6914; // 古铜金外框
    private static final int COLOR_GOLD          = 0xFFC9A227; // 亮金
    private static final int COLOR_GOLD_DIM      = 0xFF8B6914; // 暗金
    private static final int COLOR_GLOW          = 0xFFFFD700; // 发光金
    private static final int COLOR_TEXT          = 0xFFFFF0E0; // 象牙白
    private static final int COLOR_RUNE          = 0xFF6B4226; // 符文棕
    private static final int COLOR_CARD_BG       = 0xFF0D0408; // 深红卡片
    private static final int COLOR_CARD_BORDER   = 0xFF5C3A1E; // 暗金边框
    private static final int COLOR_BTN_BG        = 0xFF2D0A12; // 深红按钮
    private static final int COLOR_BTN_HOVER     = 0xFF4A1020; // 悬停
    private static final int COLOR_TRACK_BG      = 0xFF0D0408; // 轨道

    private static final int PANEL_WIDTH  = 300;
    private static final int PANEL_HEIGHT = 340;

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
        int cy = panelY + 110;

        double ySliderValue = (double) (currentYOffset - MIN_Y) / (MAX_Y - MIN_Y);
        addDrawableChild(new TimeLordSlider(
                cx, cy, 252, 20,
                Text.translatable("gui.doctor_m.obelisk.y_offset", currentYOffset), ySliderValue
        ) {
            @Override protected void updateMessage() {
                int actual = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                setMessage(Text.translatable("gui.doctor_m.obelisk.y_offset", actual));
            }
            @Override protected void applyValue() {
                currentYOffset = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                blockEntity.setYOffset(currentYOffset);
                syncToServer();
            }
        });

        double scaleSliderValue = (double) (currentScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        addDrawableChild(new TimeLordSlider(
                cx, cy + 42, 252, 20,
                Text.translatable("gui.doctor_m.obelisk.scale", String.format("%.1f", currentScale)), scaleSliderValue
        ) {
            @Override protected void updateMessage() {
                float actual = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                actual = Math.round(actual * 10.0f) / 10.0f;
                setMessage(Text.translatable("gui.doctor_m.obelisk.scale", String.format("%.1f", actual)));
            }
            @Override protected void applyValue() {
                currentScale = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                currentScale = Math.round(currentScale * 10.0f) / 10.0f;
                blockEntity.setScale(currentScale);
                syncToServer();
            }
        });

        addDrawableChild(new TimeLordCheck(
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

        addDrawableChild(new TimeLordCheck(
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

        addDrawableChild(new TimeLordButton(
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = panelX + 24;

        // 全屏遮罩
        ctx.fill(0, 0, this.width, this.height, COLOR_OVERLAY);

        // ========== 面板主体（深酒红 + 古铜金粗边框） ==========
        ctx.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF080808);
        ctx.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL);
        // 3px 古铜外框
        drawThickBorder(ctx, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, COLOR_PANEL_BORDER, 2);
        // 内层金线
        ctx.drawBorder(panelX + 2, panelY + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, COLOR_GOLD_DIM);

        // ========== 顶部装饰==========
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.obelisk.gallifrey").formatted(Formatting.GOLD),
                panelX + PANEL_WIDTH / 2, panelY + 6, COLOR_GOLD_DIM);

        // 主标题
        String title = Text.translatable("gui.doctor_m.obelisk.title").getString();
        int titleWidth = this.textRenderer.getWidth(title);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(title).formatted(Formatting.WHITE),
                panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 18, COLOR_TEXT);

        // 标题下符文分隔线（左右对称装饰）
        int lineY = panelY + 32;
        drawRuneLine(ctx, panelX + 20, lineY, PANEL_WIDTH - 40);

        // ========== 能量卡片（深红底 + 金边） ==========
        int cardX = panelX + 16;
        int cardY = panelY + 38;
        int cardW = PANEL_WIDTH - 32;
        int cardH = 54;

        ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_CARD_BG);
        ctx.drawBorder(cardX, cardY, cardW, cardH, COLOR_CARD_BORDER);

        // 小标签
        ctx.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.obelisk.eye_of_harmony").formatted(Formatting.GOLD),
                cardX + 10, cardY + 6, COLOR_GOLD_DIM);

        // 能量条
        int barX = cardX + 10, barY = cardY + 20, barW = cardW - 20, barH = 8;
        double percentage = blockEntity.getEnergyPercentage();
        int filledWidth = (int) (barW * percentage);

        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0406);
        ctx.drawBorder(barX, barY, barW, barH, COLOR_CARD_BORDER);
        if (filledWidth > 0) {
            int color = getEnergyColor(percentage);
            ctx.fill(barX + 1, barY + 1, barX + filledWidth - 1, barY + barH - 1, color);
            // 顶部金色光泽
            ctx.fill(barX + 1, barY + 1, barX + filledWidth - 1, barY + 3, 0xFFFFD700);
        }

        // 能量条文字
        Text energyText = Text.translatable("gui.doctor_m.obelisk.energy",
                String.format("%.0f", blockEntity.getCurrentFuel()),
                String.format("%.0f", blockEntity.getMaxFuel()),
                String.format("%.0f", percentage * 100)).formatted(Formatting.WHITE);
        int textX = panelX + (PANEL_WIDTH - textRenderer.getWidth(energyText)) / 2;
        ctx.drawTextWithShadow(this.textRenderer, energyText, textX, barY + 12, COLOR_TEXT);

        // ========== 状态区 ==========
        int statusY = panelY + PANEL_HEIGHT - 58;
        // 符文分隔线
        drawRuneLine(ctx, panelX + 20, statusY - 10, PANEL_WIDTH - 40);

        boolean active = blockEntity.isActive();
        // 状态指示灯（金色/暗色圆点）
        int dotX = panelX + (PANEL_WIDTH - 6) / 2 - 35;
        int dotColor = active ? COLOR_GLOW : COLOR_GOLD_DIM;
        ctx.fill(dotX, statusY, dotX + 6, statusY + 6, dotColor);
        ctx.fill(dotX - 1, statusY - 1, dotX + 7, statusY + 7, dotColor & 0x40FFFFFF);

        String statusKey = active
                ? "gui.doctor_m.obelisk.active"
                : "gui.doctor_m.obelisk.inactive";
        Text statusText = Text.translatable("gui.doctor_m.obelisk.status",
                Text.translatable(statusKey).formatted(active ? Formatting.GOLD : Formatting.RED));
        int statusWidth = this.textRenderer.getWidth(statusText);
        ctx.drawTextWithShadow(this.textRenderer, statusText,
                panelX + (PANEL_WIDTH - statusWidth) / 2 + 6, statusY + 1, COLOR_TEXT);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ========== Gallifreyan 符文装饰线 ==========
    private void drawRuneLine(DrawContext ctx, int x, int y, int w) {
        int center = x + w / 2;
        // 左半部分：点-线-点-线
        ctx.fill(x, y, x + w / 2 - 8, y + 1, COLOR_RUNE);
        ctx.fill(x + w / 2 + 8, y, x + w, y + 1, COLOR_RUNE);
        // 中心：菱形装饰
        ctx.fill(center - 2, y - 2, center + 2, y + 3, COLOR_GOLD_DIM);
        ctx.fill(center - 1, y - 1, center + 1, y + 2, COLOR_GLOW);
        // 两侧小圆点
        ctx.fill(x + 4, y - 1, x + 6, y + 2, COLOR_GOLD_DIM);
        ctx.fill(x + w - 6, y - 1, x + w - 4, y + 2, COLOR_GOLD_DIM);
    }

    private void drawThickBorder(DrawContext ctx, int x, int y, int w, int h, int color, int thickness) {
        for (int i = 0; i < thickness; i++) {
            ctx.drawBorder(x + i, y + i, w - i * 2, h - i * 2, color);
        }
    }

    private int getEnergyColor(double percentage) {
        // 0% → 暗红  50% → 橙金  100% → 亮金
        int r, g;
        if (percentage < 0.5) {
            r = 0x8B + (int)((0xFF - 0x8B) * percentage * 2);
            g = (int)(0x40 * percentage * 2);
        } else {
            r = 0xFF;
            g = 0x40 + (int)((0xD7 - 0x40) * (percentage - 0.5) * 2);
        }
        return 0xFF000000 | (r << 16) | (g << 8) | 0x00;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ========== Time Lord 风格滑块 ==========
    private abstract class TimeLordSlider extends SliderWidget {
        TimeLordSlider(int x, int y, int w, int h, Text msg, double val) {
            super(x, y, w, h, msg, val);
        }

        @Override
        public void renderButton(DrawContext ctx, int mx, int my, float delta) {
            int tx = this.getX();
            int ty = this.getY();
            int tw = this.getWidth();

            ctx.drawTextWithShadow(textRenderer, getMessage(), tx, ty - 10, COLOR_TEXT);

            int trackY = ty + 6;
            int trackH = 4;

            ctx.fill(tx, trackY, tx + tw, trackY + trackH, COLOR_TRACK_BG);
            ctx.drawBorder(tx, trackY, tw, trackH, COLOR_CARD_BORDER);

            int fillW = (int) (tw * this.value);
            if (fillW > 0) {
                ctx.fill(tx + 1, trackY + 1, tx + fillW - 1, trackY + trackH - 1, COLOR_GOLD);
                ctx.fill(tx + 1, trackY + 1, tx + fillW - 1, trackY + 2, COLOR_GLOW);
            }

            int handleW = 8;
            int handleH = 12;
            int hx = tx + fillW - handleW / 2;
            hx = MathHelper.clamp(hx, tx - 2, tx + tw - handleW + 2);
            int hy = trackY - (handleH - trackH) / 2;

            boolean hovered = isHovered();
            int hc = hovered ? COLOR_GLOW : COLOR_GOLD;
            ctx.fill(hx, hy, hx + handleW, hy + handleH, hc);
            ctx.drawBorder(hx, hy, handleW, handleH, hovered ? COLOR_GLOW : COLOR_GOLD_DIM);
            ctx.fill(hx + 3, hy + 3, hx + 5, hy + handleH - 3, COLOR_PANEL);
        }
    }

    // ========== Time Lord 风格复选框 ==========
    private abstract class TimeLordCheck extends CheckboxWidget {
        TimeLordCheck(int x, int y, int w, int h, Text msg, boolean checked) {
            super(x, y, w, h, msg, checked);
        }

        @Override
        public void renderButton(DrawContext ctx, int mx, int my, float delta) {
            int tx = this.getX();
            int ty = this.getY();
            int box = 12;

            ctx.fill(tx, ty, tx + box, ty + box, COLOR_CARD_BG);
            boolean hovered = isHovered();
            ctx.drawBorder(tx, ty, box, box, hovered ? COLOR_GOLD : COLOR_CARD_BORDER);

            if (isChecked()) {
                ctx.fill(tx + 2, ty + 2, tx + box - 2, ty + box - 2, COLOR_GOLD);
                // 对勾用深色
                ctx.fill(tx + 3, ty + 6, tx + 5, ty + 8, COLOR_PANEL);
                ctx.fill(tx + 5, ty + 4, tx + 7, ty + 6, COLOR_PANEL);
                ctx.fill(tx + 7, ty + 2, tx + 9, ty + 4, COLOR_PANEL);
            }

            ctx.drawTextWithShadow(textRenderer, getMessage(), tx + box + 6, ty + 2, COLOR_TEXT);
        }
    }

    // ========== Time Lord 风格按钮 ==========
    private class TimeLordButton extends ButtonWidget {
        TimeLordButton(int x, int y, int w, int h, Text msg, PressAction action) {
            super(x, y, w, h, msg, action, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        public void renderButton(DrawContext ctx, int mx, int my, float delta) {
            int tx = getX(), ty = getY(), tw = getWidth(), th = getHeight();
            boolean hovered = isHovered();

            int bg = hovered ? COLOR_BTN_HOVER : COLOR_BTN_BG;
            ctx.fill(tx, ty, tx + tw, ty + th, bg);
            ctx.drawBorder(tx, ty, tw, th, hovered ? COLOR_GOLD : COLOR_CARD_BORDER);

            if (hovered) {
                ctx.fill(tx + 1, ty + 1, tx + tw - 1, ty + 2, COLOR_GLOW);
            }

            Text msg = getMessage();
            int mw = textRenderer.getWidth(msg);
            int color = this.active ? (hovered ? COLOR_GLOW : COLOR_TEXT) : 0xFF555555;
            ctx.drawTextWithShadow(textRenderer, msg, tx + (tw - mw) / 2, ty + (th - 8) / 2, color);
        }
    }
}