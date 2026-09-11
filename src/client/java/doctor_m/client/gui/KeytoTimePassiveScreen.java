package doctor_m.client.gui;

import doctor_m.Item.data_item.KeytoTimeItem;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.handler.KeytoTime.KeytoTimePassive;
import doctor_m.network.KeytoTimeNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class KeytoTimePassiveScreen extends Screen {

    // ===== 布局常量 =====
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 264;
    private static final int HEADER_H = 26;
    private static final int PAD = 14;
    private static final int BTN_H = 22;
    private static final int ROW_GAP = 5;
    private static final int MODE_PANEL_H = 92;
    private static final int TITLE_PANEL_H = 76;

    // ===== 配色 =====
    private static final int C_BG            = 0xF0000000;  // 纯黑底
    private static final int C_BORDER        = 0xFFB088FF;  // 淡紫边框
    private static final int C_BORDER_GLOW   = 0x50B088FF;  // 淡紫内发光
    private static final int C_ACCENT        = 0xFFD0A8FF;  // 强调色（浅紫）
    private static final int C_HEADER_BG     = 0x60000000;
    private static final int C_PANEL_INNER   = 0x25FFFFFF;
    private static final int C_PANEL_BORDER  = 0x40FFFFFF;
    private static final int C_DIVIDER       = 0x30FFFFFF;
    private static final int C_TEXT          = 0xFFFFFFFF;
    private static final int C_TEXT_DIM      = 0xFFAA99BB;

    private final PlayerEntity player;
    private ButtonWidget godModeBtn, neutralBtn, slashBtn, saveTitleBtn;
    private TextFieldWidget titleField;

    // 运行时布局
    private int panelX, panelY;
    private int modePanelY, titlePanelY, titlePanelH;
    private int titleLabelY, titleFieldY;
    private int closeBtnY;

    public KeytoTimePassiveScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.key_to_time.passive.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        if (KeytoTimeCore.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }

        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;

        int contentX = panelX + PAD;
        int contentW = PANEL_W - 2 * PAD;

        // ===== 模式面板 =====
        this.modePanelY = panelY + HEADER_H + 12;
        int modeBtnY = modePanelY + 8;

        godModeBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.key_to_time.godmode_status", KeytoTimePassive.isGodMode(player)),
                btn -> send(0)
        ).position(contentX, modeBtnY).size(contentW, BTN_H).build());

        modeBtnY += BTN_H + ROW_GAP;
        neutralBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.key_to_time.neutral_status", KeytoTimePassive.isNeutralMode(player)),
                btn -> send(1)
        ).position(contentX, modeBtnY).size(contentW, BTN_H).build());

        modeBtnY += BTN_H + ROW_GAP;
        slashBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.key_to_time.slash_mode", KeytoTimePassive.isSlashMode(player)),
                btn -> send(2)
        ).position(contentX, modeBtnY).size(contentW, BTN_H).build());

        // ===== 标题面板 =====
        this.titlePanelY = modePanelY + MODE_PANEL_H + 12;
        this.titlePanelH = TITLE_PANEL_H;
        this.titleLabelY = titlePanelY + 8;
        this.titleFieldY = titleLabelY + 12;

        ItemStack keyStack = KeytoTimeCore.getTimeKeyStack(player);
        String currentTitle = KeytoTimeItem.getTitle(keyStack);
        if (currentTitle == null) currentTitle = "";

        titleField = new TextFieldWidget(this.textRenderer, contentX, titleFieldY, contentW, 20,
                Text.translatable("gui.doctor_m.title.hint"));
        titleField.setMaxLength(32);
        titleField.setText(currentTitle);
        titleField.setPlaceholder(Text.translatable("gui.doctor_m.title.placeholder"));
        this.addDrawableChild(titleField);

        int saveBtnY = titleFieldY + 20 + 6;
        saveTitleBtn = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.title.save"),
                btn -> sendTitle()
        ).position(contentX, saveBtnY).size(contentW, BTN_H).build());

        // ===== 关闭按钮 =====
        this.closeBtnY = panelY + PANEL_H - BTN_H - PAD;
        int closeBtnW = 100;
        int closeBtnX = panelX + (PANEL_W - closeBtnW) / 2;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(closeBtnX, closeBtnY).size(closeBtnW, BTN_H).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (KeytoTimeCore.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }
        godModeBtn.setMessage(toggleText("gui.doctor_m.key_to_time.godmode_status", KeytoTimePassive.isGodMode(player)));
        neutralBtn.setMessage(toggleText("gui.doctor_m.key_to_time.neutral_status", KeytoTimePassive.isNeutralMode(player)));
        slashBtn.setMessage(toggleText("gui.doctor_m.key_to_time.slash_mode", KeytoTimePassive.isSlashMode(player)));
        titleField.tick();
    }

    private MutableText toggleText(String key, boolean enabled) {
        Text state = enabled
                ? Text.translatable("key.doctor_m.mode.on").formatted(Formatting.GREEN, Formatting.BOLD)
                : Text.translatable("key.doctor_m.mode.off").formatted(Formatting.RED, Formatting.BOLD);
        return Text.translatable(key, state);
    }

    private void send(int featureId) {
        var buf = PacketByteBufs.create();
        buf.writeInt(featureId);
        ClientPlayNetworking.send(KeytoTimeNetwork.TOGGLE_PASSIVE, buf);
    }

    private void sendTitle() {
        String title = titleField.getText().trim();

        ItemStack keyStack = KeytoTimeCore.getTimeKeyStack(player);
        KeytoTimeItem.setTitle(keyStack, title);

        var buf = PacketByteBufs.create();
        buf.writeString(title, 64);
        ClientPlayNetworking.send(KeytoTimeNetwork.SET_TITLE, buf);
    }

    @Override
    public void renderBackground(DrawContext ctx) {
        // 禁用原版遮罩
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int px = panelX, py = panelY;
        int pr = px + PANEL_W, pb = py + PANEL_H;

        // ===== 阴影 =====
        ctx.fill(px - 3, py - 3, pr + 3, pb + 3, 0x40000000);
        ctx.fill(px - 2, py - 2, pr + 2, pb + 2, 0x50000000);
        ctx.fill(px - 1, py - 1, pr + 1, pb + 1, 0x80000000);

        // ===== 背景 =====
        ctx.fill(px, py, pr, pb, C_BG);

        // ===== 淡紫色边框 =====
        ctx.fill(px, py, pr, py + 1, C_BORDER);
        ctx.fill(px, pb - 1, pr, pb, C_BORDER);
        ctx.fill(px, py, px + 1, pb, C_BORDER);
        ctx.fill(pr - 1, py, pr, pb, C_BORDER);

        // ===== 内发光 =====
        ctx.fill(px + 1, py + 1, pr - 1, py + 2, C_BORDER_GLOW);
        ctx.fill(px + 1, py + 1, px + 2, pb - 1, C_BORDER_GLOW);
        ctx.fill(pr - 2, py + 1, pr - 1, pb - 1, C_BORDER_GLOW);
        ctx.fill(px + 1, pb - 2, pr - 1, pb - 1, C_BORDER_GLOW);

        // ===== 标题栏 =====
        ctx.fill(px + 1, py + 1, pr - 1, py + HEADER_H, C_HEADER_BG);
        ctx.fill(px + 1, py + HEADER_H, pr - 1, py + HEADER_H + 1, C_ACCENT);

        MutableText titleText = Text.literal("✦ ")
                .formatted(Formatting.LIGHT_PURPLE)
                .append(this.title.copy().formatted(Formatting.WHITE))
                .append(Text.literal(" ✦").formatted(Formatting.LIGHT_PURPLE));
        ctx.drawCenteredTextWithShadow(this.textRenderer, titleText,
                px + PANEL_W / 2, py + 8, 0xFFFFFF);

        // ===== 模式面板 =====
        drawInnerPanel(ctx,
                px + PAD - 6, modePanelY,
                PANEL_W - 2 * PAD + 12, MODE_PANEL_H);

        // ===== 标题面板 =====
        drawInnerPanel(ctx,
                px + PAD - 6, titlePanelY,
                PANEL_W - 2 * PAD + 12, titlePanelH);

        // 标题标签
        ctx.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.title.label"),
                px + PAD, titleLabelY, C_ACCENT);

        // ===== 底部分隔线 =====
        ctx.fill(px + PAD, closeBtnY - 8, pr - PAD, closeBtnY - 7, C_DIVIDER);

        super.render(ctx, mx, my, delta);
    }

    /** 绘制带边框的内嵌面板 */
    private void drawInnerPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, C_PANEL_INNER);
        ctx.fill(x, y, x + w, y + 1, C_PANEL_BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, C_PANEL_BORDER);
        ctx.fill(x, y, x + 1, y + h, C_PANEL_BORDER);
        ctx.fill(x + w - 1, y, x + w, y + h, C_PANEL_BORDER);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}