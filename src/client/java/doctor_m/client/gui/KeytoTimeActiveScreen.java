package doctor_m.client.gui;

import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.network.KeytoTimeActiveNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class KeytoTimeActiveScreen extends Screen {

    // ===== 布局常量 =====
    private static final int PANEL_W = 240;
    private static final int PANEL_H = 200;
    private static final int HEADER_H = 26;
    private static final int PAD = 16;
    private static final int BTN_H = 22;
    private static final int ROW_GAP = 6;
    private static final int ACTION_PANEL_H = 94;

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

    private final PlayerEntity player;

    private ButtonWidget gameModeBtn, difficultyBtn, teleportBtn;

    // 运行时布局
    private int panelX, panelY;
    private int actionPanelY;
    private int closeBtnY;

    public KeytoTimeActiveScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.key_to_time.active.title"));
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

        // ===== 功能面板 =====
        this.actionPanelY = panelY + HEADER_H + 12;
        int btnY = actionPanelY + 8;

        gameModeBtn = this.addDrawableChild(ButtonWidget.builder(
                buildGameModeText(),
                btn -> send(0)
        ).position(contentX, btnY).size(contentW, BTN_H).build());

        btnY += BTN_H + ROW_GAP;
        difficultyBtn = this.addDrawableChild(ButtonWidget.builder(
                buildDifficultyText(),
                btn -> send(1)
        ).position(contentX, btnY).size(contentW, BTN_H).build());

        btnY += BTN_H + ROW_GAP + 4;
        teleportBtn = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.key_to_time.active.teleport")
                        .formatted(Formatting.AQUA),
                btn -> MinecraftClient.getInstance().setScreen(new KeytoTimeTeleportScreen(player))
        ).position(contentX, btnY).size(contentW, BTN_H).build());

        // ===== 关闭按钮 =====
        this.closeBtnY = panelY + PANEL_H - BTN_H - 14;
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
        if (gameModeBtn != null) gameModeBtn.setMessage(buildGameModeText());
        if (difficultyBtn != null) difficultyBtn.setMessage(buildDifficultyText());
    }

    private Text buildGameModeText() {
        var client = MinecraftClient.getInstance();
        var mode = client.interactionManager != null ? client.interactionManager.getCurrentGameMode() : null;
        String modeName = mode != null ? Text.translatable("selectWorld.gameMode." + mode.getName()).getString() : "?";
        return Text.translatable("gui.doctor_m.key_to_time.active.gamemode")
                .append(" [")
                .append(Text.literal(modeName).formatted(Formatting.YELLOW, Formatting.BOLD))
                .append("]");
    }

    private Text buildDifficultyText() {
        var client = MinecraftClient.getInstance();
        var diff = client.world != null ? client.world.getDifficulty() : null;
        String diffName = diff != null ? Text.translatable("options.difficulty." + diff.getName()).getString() : "?";
        return Text.translatable("gui.doctor_m.key_to_time.active.difficulty")
                .append(" [")
                .append(Text.literal(diffName).formatted(Formatting.YELLOW, Formatting.BOLD))
                .append("]");
    }

    private void send(int abilityId) {
        var buf = PacketByteBufs.create();
        buf.writeInt(abilityId);
        ClientPlayNetworking.send(KeytoTimeActiveNetwork.ACTIVE_ABILITY, buf);
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

        // ===== 功能面板 =====
        drawInnerPanel(ctx,
                px + PAD - 6, actionPanelY,
                PANEL_W - 2 * PAD + 12, ACTION_PANEL_H);

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