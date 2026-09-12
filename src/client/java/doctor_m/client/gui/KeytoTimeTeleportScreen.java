package doctor_m.client.gui;

import doctor_m.network.KeytoTimeTeleportNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class KeytoTimeTeleportScreen extends Screen {

    // ===== 布局常量 =====
    private static final int PANEL_W = 360;              // 从 340 加宽
    private static final int PANEL_H = 195;
    private static final int HEADER_H = 26;
    private static final int PAD = 16;
    private static final int COL_GAP = 24;               // 从 10 增大，避免重叠
    private static final int LABEL_W = 14;
    private static final int LABEL_GAP = 8;
    private static final int FIELD_W = 110;
    private static final int FIELD_H = 18;
    private static final int ROW_SPACING = 26;
    private static final int BTN_H = 20;
    private static final int DIM_PANEL_H = 64;           // 维度面板高度（容纳索引行）

    // ===== 配色 =====
    private static final int C_BG            = 0xF0000000;
    private static final int C_BORDER        = 0xFFB088FF;
    private static final int C_BORDER_GLOW   = 0x50B088FF;
    private static final int C_ACCENT        = 0xFFD0A8FF;
    private static final int C_HEADER_BG     = 0x60000000;
    private static final int C_PANEL_INNER   = 0x25FFFFFF;
    private static final int C_PANEL_BORDER  = 0x40FFFFFF;
    private static final int C_DIVIDER       = 0x30FFFFFF;
    private static final int C_TEXT          = 0xFFFFFFFF;
    private static final int C_TEXT_DIM      = 0xFFAA99BB;

    private final PlayerEntity player;
    private TextFieldWidget xField, yField, zField;

    private List<String> availableDims = new ArrayList<>();
    private int dimIndex = 0;
    private boolean dimsLoaded = false;

    // 运行时布局
    private int panelX, panelY;
    private int leftColX, rightColX, rightW;
    private int zFieldY, yFieldY, xFieldY;
    private int fieldX;
    private int dimLabelY, dimRowY;
    private int bottomBtnY;

    public KeytoTimeTeleportScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.key_to_time.teleport.title"));
        this.player = player;
        ClientPlayNetworking.send(KeytoTimeTeleportNetwork.REQUEST_DIMS, PacketByteBufs.empty());
    }

    public void onDimensionsReceived(List<String> dims) {
        this.availableDims = dims;
        this.dimsLoaded = true;
        String current = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
        int idx = availableDims.indexOf(current);
        this.dimIndex = idx >= 0 ? idx : 0;
    }

    @Override
    protected void init() {
        super.init();

        // 面板居中
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;

        // 左右列
        this.leftColX = panelX + PAD;
        this.rightColX = leftColX + (LABEL_W + LABEL_GAP + FIELD_W) + COL_GAP;
        this.rightW = panelX + PANEL_W - PAD - rightColX;

        // 坐标字段
        this.fieldX = leftColX + LABEL_W + LABEL_GAP;
        this.zFieldY = panelY + HEADER_H + 14;
        this.yFieldY = zFieldY + ROW_SPACING;
        this.xFieldY = yFieldY + ROW_SPACING;

        // 维度
        this.dimLabelY = panelY + HEADER_H + 12;
        this.dimRowY = dimLabelY + 16;

        // 底部
        this.bottomBtnY = panelY + PANEL_H - BTN_H - 14;

        // ===== 坐标输入 =====
        this.zField = new TextFieldWidget(this.textRenderer, fieldX, zFieldY, FIELD_W, FIELD_H, Text.literal("Z"));
        this.zField.setText(String.valueOf((int) player.getZ()));
        this.addDrawableChild(this.zField);

        this.yField = new TextFieldWidget(this.textRenderer, fieldX, yFieldY, FIELD_W, FIELD_H, Text.literal("Y"));
        this.yField.setText(String.valueOf((int) player.getY()));
        this.addDrawableChild(this.yField);

        this.xField = new TextFieldWidget(this.textRenderer, fieldX, xFieldY, FIELD_W, FIELD_H, Text.literal("X"));
        this.xField.setText(String.valueOf((int) player.getX()));
        this.addDrawableChild(this.xField);

        // ===== 维度切换 =====
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> cycleDim(-1))
                .position(rightColX, dimRowY).size(20, FIELD_H).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> cycleDim(1))
                .position(rightColX + rightW - 20, dimRowY).size(20, FIELD_H).build());

        // ===== 底部按钮 =====
        int bottomLeftX = panelX + (PANEL_W - 140 * 2 - 20) / 2;
        int bottomRightX = bottomLeftX + 140 + 20;

        // 左：关闭
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(bottomLeftX, bottomBtnY).size(140, BTN_H).build());

        // 右：前往
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.go").formatted(Formatting.GREEN, Formatting.BOLD),
                btn -> attemptTeleport()
        ).position(bottomRightX, bottomBtnY).size(140, BTN_H).build());
    }

    private void cycleDim(int dir) {
        if (availableDims.isEmpty()) return;
        dimIndex = (dimIndex + dir + availableDims.size()) % availableDims.size();
    }

    private void attemptTeleport() {
        if (availableDims.isEmpty()) return;
        try {
            double x = Double.parseDouble(this.xField.getText());
            double y = Double.parseDouble(this.yField.getText());
            double z = Double.parseDouble(this.zField.getText());

            var buf = PacketByteBufs.create();
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeString(availableDims.get(dimIndex));
            ClientPlayNetworking.send(KeytoTimeTeleportNetwork.TELEPORT, buf);
            this.close();
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void renderBackground(DrawContext ctx) {
        // 禁用原版遮罩
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
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
        ctx.drawCenteredTextWithShadow(this.textRenderer, titleText, px + PANEL_W / 2, py + 8, 0xFFFFFF);

        // ===== 坐标面板 =====
        drawInnerPanel(ctx,
                leftColX - 8, zFieldY - 8,
                LABEL_W + LABEL_GAP + FIELD_W + 16,
                ROW_SPACING * 3 + 4);

        ctx.drawTextWithShadow(this.textRenderer, "Z", leftColX, zFieldY + 5, C_ACCENT);
        ctx.drawTextWithShadow(this.textRenderer, "Y", leftColX, yFieldY + 5, C_ACCENT);
        ctx.drawTextWithShadow(this.textRenderer, "X", leftColX, xFieldY + 5, C_ACCENT);

        // ===== 维度面板 =====
        drawInnerPanel(ctx,
                rightColX - 8, dimLabelY - 8,
                rightW + 16,
                DIM_PANEL_H);

        ctx.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.vm.dimension"),
                rightColX, dimLabelY, C_ACCENT);

        Text dimText;
        if (availableDims.isEmpty()) {
            dimText = Text.literal("-").formatted(Formatting.RED);
        } else {
            dimText = getDimensionText(availableDims.get(dimIndex));
        }

        int dimTextW = this.textRenderer.getWidth(dimText);
        int dimCenterX = rightColX + rightW / 2;
        ctx.drawTextWithShadow(this.textRenderer, dimText,
                dimCenterX - dimTextW / 2, dimRowY + 4, C_TEXT);

        // 维度指示器
        if (availableDims.size() > 1) {
            String indexText = (dimIndex + 1) + " / " + availableDims.size();
            Text idxText = Text.literal(indexText).formatted(Formatting.GRAY);
            int idxW = this.textRenderer.getWidth(idxText);
            ctx.drawTextWithShadow(this.textRenderer, idxText,
                    dimCenterX - idxW / 2, dimRowY + FIELD_H + 6, 0xAAAAAA);
        }

        // ===== 底部分隔线 =====
        ctx.fill(px + PAD, bottomBtnY - 10, pr - PAD, bottomBtnY - 9, C_DIVIDER);

        super.render(ctx, mouseX, mouseY, delta);
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

    private static Text getDimensionText(String dimId) {
        if (dimId == null || dimId.isEmpty()) return Text.literal("-");
        try {
            Identifier id = new Identifier(dimId);
            String key = "dimension." + id.getNamespace() + "." + id.getPath();
            String translated = net.minecraft.client.resource.language.I18n.translate(key);
            if (translated.equals(key)) return Text.literal(dimId);
            return Text.literal(translated);
        } catch (Exception e) {
            return Text.literal(dimId);
        }
    }
}