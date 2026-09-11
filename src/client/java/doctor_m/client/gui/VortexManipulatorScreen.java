package doctor_m.client.gui;

import doctor_m.Item.data_item.VortexManipulatorItem;
import doctor_m.Item.items;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.network.VMNetwork;
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
import net.minecraft.util.Identifier;

public class VortexManipulatorScreen extends Screen {

    // ===== 布局常量 =====
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 210;
    private static final int HEADER_H = 26;
    private static final int PAD = 14;
    private static final int COL_GAP = 24;
    private static final int LABEL_W = 16;
    private static final int LABEL_GAP = 6;
    private static final int FIELD_W = 120;
    private static final int FIELD_H = 18;
    private static final int ROW_SPACING = 26;
    private static final int BTN_H = 20;
    private static final int BTN_W = 140;

    // ===== 配色 =====
    private static final int C_BG           = 0xEE0A0512;
    private static final int C_BORDER       = 0xFF7B3FB0;
    private static final int C_BORDER_GLOW  = 0x40B070FF;
    private static final int C_ACCENT       = 0xFF00D4FF;
    private static final int C_HEADER_BG    = 0x60000000;
    private static final int C_PANEL_INNER  = 0x25000000;
    private static final int C_PANEL_BORDER = 0x30FFFFFF;
    private static final int C_DIVIDER      = 0x30FFFFFF;
    private static final int C_TEXT         = 0xFFFFFFFF;
    private static final int C_TEXT_DIM     = 0xFF9A8AA8;

    private static final ModConfig CONFIG = ConfigManager.getConfig();

    private final PlayerEntity player;
    private TextFieldWidget xField, yField, zField;
    private ButtonWidget goBtn;   // ★ 传送按钮引用

    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private String lastDim = null;

    // ===== 运行时布局 =====
    private int panelX, panelY;
    private int leftColX, rightColX, rightW;
    private int zFieldY, yFieldY, xFieldY;
    private int fieldX;
    private int dimLabelY, dimRowY;
    private int presetY1, presetY2;
    private int fuelY, heatY;
    private int bottomBtnY;

    public VortexManipulatorScreen(PlayerEntity player, ItemStack stack) {
        super(Text.translatable("gui.doctor_m.vm.title"));
        this.player = player;
    }

    private ItemStack getVMStack() {
        ItemStack main = player.getMainHandStack();
        if (main.isOf(items.VORTEX_MANIPULATOR)) return main;
        ItemStack off = player.getOffHandStack();
        if (off.isOf(items.VORTEX_MANIPULATOR)) return off;
        return ItemStack.EMPTY;
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
        this.presetY1 = dimRowY + 28;
        this.presetY2 = presetY1 + 26;

        // 燃料 / 热量
        this.fuelY = xFieldY + ROW_SPACING + 14;
        this.heatY = fuelY + 16;

        // 底部
        this.bottomBtnY = panelY + PANEL_H - BTN_H - 14;

        ItemStack stack = getVMStack();
        if (stack.isEmpty()) {
            this.close();
            return;
        }

        // ===== 坐标输入 =====
        this.zField = new TextFieldWidget(this.textRenderer, fieldX, zFieldY, FIELD_W, FIELD_H, Text.literal("Z"));
        this.zField.setText(String.valueOf((int) VortexManipulatorItem.getDestZ(stack)));
        this.addDrawableChild(this.zField);

        this.yField = new TextFieldWidget(this.textRenderer, fieldX, yFieldY, FIELD_W, FIELD_H, Text.literal("Y"));
        this.yField.setText(String.valueOf((int) VortexManipulatorItem.getDestY(stack)));
        this.addDrawableChild(this.yField);

        this.xField = new TextFieldWidget(this.textRenderer, fieldX, xFieldY, FIELD_W, FIELD_H, Text.literal("X"));
        this.xField.setText(String.valueOf((int) VortexManipulatorItem.getDestX(stack)));
        this.addDrawableChild(this.xField);

        // ===== 维度切换 =====
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> {
            var buf = PacketByteBufs.create();
            buf.writeBoolean(true);
            ClientPlayNetworking.send(VMNetwork.CYCLE_DIM, buf);
        }).position(rightColX, dimRowY).size(20, FIELD_H).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> {
            var buf = PacketByteBufs.create();
            buf.writeBoolean(false);
            ClientPlayNetworking.send(VMNetwork.CYCLE_DIM, buf);
        }).position(rightColX + rightW - 20, dimRowY).size(20, FIELD_H).build());

        // ===== 预设按钮 =====
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.set_current"),
                btn -> ClientPlayNetworking.send(VMNetwork.SET_CURRENT_DEST, PacketByteBufs.empty())
        ).position(rightColX, presetY1).size(rightW, BTN_H).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.set_prev"),
                btn -> ClientPlayNetworking.send(VMNetwork.SET_PREV_DEST, PacketByteBufs.empty())
        ).position(rightColX, presetY2).size(rightW, BTN_H).build());

        // ===== 底部按钮 =====
        int bottomLeftX = panelX + (PANEL_W - BTN_W * 2 - 20) / 2;
        int bottomRightX = bottomLeftX + BTN_W + 20;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(bottomLeftX, bottomBtnY).size(BTN_W, BTN_H).build());

        // ★ 保存传送按钮引用，后续 tick 动态更新
        this.goBtn = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.go").formatted(Formatting.GREEN, Formatting.BOLD),
                btn -> attemptTeleport()
        ).position(bottomRightX, bottomBtnY).size(BTN_W, BTN_H).build());

        syncLastValues(stack);

        // 初始化时刷新传送按钮状态
        updateGoButtonState(stack);
    }

    /** ★ 根据冷却/破损状态更新传送按钮的显示与可用性 */
    private void updateGoButtonState(ItemStack stack) {
        if (goBtn == null || stack.isEmpty()) return;

        boolean broken = VortexManipulatorItem.isBroken(stack);
        boolean onCooldown = VortexManipulatorItem.isOnCooldownSys(stack);

        if (broken) {
            goBtn.setMessage(Text.translatable("gui.doctor_m.vm.broken_label")
                    .formatted(Formatting.DARK_RED, Formatting.BOLD));
            goBtn.active = false;
            return;
        }

        if (onCooldown) {
            long remainingMs = VortexManipulatorItem.getCooldownEndSys(stack) - System.currentTimeMillis();
            int sec = Math.max(1, (int) Math.ceil(remainingMs / 1000.0));
            goBtn.setMessage(Text.translatable("gui.doctor_m.vm.go.cooldown", sec)
                    .formatted(Formatting.RED));
            goBtn.active = false;
            return;
        }

        goBtn.setMessage(Text.translatable("gui.doctor_m.vm.go")
                .formatted(Formatting.GREEN, Formatting.BOLD));
        goBtn.active = true;
    }

    private void syncLastValues(ItemStack stack) {
        lastX = (int) VortexManipulatorItem.getDestX(stack);
        lastY = (int) VortexManipulatorItem.getDestY(stack);
        lastZ = (int) VortexManipulatorItem.getDestZ(stack);
        lastDim = VortexManipulatorItem.getDestDim(stack);
    }

    private void attemptTeleport() {
        try {
            double x = Double.parseDouble(this.xField.getText());
            double y = Double.parseDouble(this.yField.getText());
            double z = Double.parseDouble(this.zField.getText());

            var buf = PacketByteBufs.create();
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            ClientPlayNetworking.send(VMNetwork.TELEPORT, buf);
            this.close();
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void renderBackground(DrawContext context) {
        // 禁用原版遮罩
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ItemStack stack = getVMStack();
        if (stack.isEmpty()) return;

        int fuel = VortexManipulatorItem.getFuel(stack);
        int overheat = VortexManipulatorItem.getOverheat(stack);
        boolean broken = VortexManipulatorItem.isBroken(stack);
        String dimId = VortexManipulatorItem.getDestDim(stack);

        int px = panelX, py = panelY;
        int pr = px + PANEL_W, pb = py + PANEL_H;

        // ===== 背景阴影 + 面板 =====
        ctx.fill(px - 3, py - 3, pr + 3, pb + 3, 0x30000000);
        ctx.fill(px - 2, py - 2, pr + 2, pb + 2, 0x50000000);
        ctx.fill(px - 1, py - 1, pr + 1, pb + 1, 0x70000000);
        ctx.fill(px, py, pr, pb, C_BG);

        // 边框
        ctx.fill(px, py, pr, py + 1, C_BORDER);
        ctx.fill(px, pb - 1, pr, pb, C_BORDER);
        ctx.fill(px, py, px + 1, pb, C_BORDER);
        ctx.fill(pr - 1, py, pr, pb, C_BORDER);

        // 内发光
        ctx.fill(px + 1, py + 1, pr - 1, py + 2, C_BORDER_GLOW);
        ctx.fill(px + 1, py + 1, px + 2, pb - 1, C_BORDER_GLOW);

        // ===== 标题栏 =====
        ctx.fill(px + 1, py + 1, pr - 1, py + HEADER_H, C_HEADER_BG);
        ctx.fill(px + 1, py + HEADER_H, pr - 1, py + HEADER_H + 1, C_ACCENT);

        MutableText titleText = Text.literal("✦ ")
                .formatted(Formatting.AQUA)
                .append(this.title.copy().formatted(Formatting.WHITE))
                .append(Text.literal(" ✦").formatted(Formatting.AQUA));
        ctx.drawCenteredTextWithShadow(this.textRenderer, titleText, px + PANEL_W / 2, py + 8, 0xFFFFFF);

        // ===== 坐标面板 =====
        drawInnerPanel(ctx,
                leftColX - 8, zFieldY - 8,
                LABEL_W + LABEL_GAP + FIELD_W + 16,
                ROW_SPACING * 3 + 4);

        ctx.drawTextWithShadow(this.textRenderer, "Z", leftColX, zFieldY + 5, C_ACCENT);
        ctx.drawTextWithShadow(this.textRenderer, "Y", leftColX, yFieldY + 5, C_ACCENT);
        ctx.drawTextWithShadow(this.textRenderer, "X", leftColX, xFieldY + 5, C_ACCENT);

        // ===== 燃料 / 热量 =====
        int statW = LABEL_W + LABEL_GAP + FIELD_W;

        float fuelRatio = fuel / (float) CONFIG.vortexManipulatorMaxFuel;
        int fuelColor;
        if (fuelRatio > 0.5f)      fuelColor = 0xFF3DDC84;
        else if (fuelRatio > 0.2f) fuelColor = 0xFFFFC107;
        else                       fuelColor = 0xFFFF5252;

        drawStatBar(ctx, leftColX, fuelY, statW, fuelRatio, fuelColor,
                "燃料", fuel + "/" + CONFIG.vortexManipulatorMaxFuel);

        float heatRatio = Math.min(1f, overheat / 100f);
        int heatColor;
        if (overheat > 80)      heatColor = 0xFFFF5252;
        else if (overheat > 50) heatColor = 0xFFFFA726;
        else                    heatColor = 0xFF3D9EFF;

        drawStatBar(ctx, leftColX, heatY, statW, heatRatio, heatColor,
                "热量", String.valueOf(overheat));

        // ===== 维度面板 =====
        drawInnerPanel(ctx,
                rightColX - 8, dimLabelY - 8,
                rightW + 16,
                presetY2 - dimLabelY + BTN_H + 12);

        ctx.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.vm.dimension"),
                rightColX, dimLabelY, C_ACCENT);

        Text dimText = getDimensionText(dimId);
        int dimTextW = this.textRenderer.getWidth(dimText);
        int dimCenterX = rightColX + rightW / 2;
        ctx.drawTextWithShadow(this.textRenderer, dimText,
                dimCenterX - dimTextW / 2, dimRowY + 4, C_TEXT);

        // ===== 底部 =====
        ctx.fill(px + PAD, bottomBtnY - 10, pr - PAD, bottomBtnY - 9, C_DIVIDER);

        if (broken) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.doctor_m.vm.broken_label")
                            .formatted(Formatting.DARK_RED, Formatting.BOLD),
                    px + PANEL_W / 2, bottomBtnY - 22, 0xFFFFFF);
        }

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

    /** 绘制“标签 + 进度条 + 数值”样式的状态行 */
    private void drawStatBar(DrawContext ctx, int x, int y, int totalW,
                             float ratio, int color, String label, String value) {
        ratio = Math.max(0f, Math.min(1f, ratio));

        int labelW = 32;
        int valueW = this.textRenderer.getWidth(value) + 4;
        int barW = totalW - labelW - valueW - 4;
        int barH = 6;
        int barY = y + 4;

        ctx.drawTextWithShadow(this.textRenderer, label, x, y, C_TEXT_DIM);

        int barX = x + labelW;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0x60000000);
        ctx.fill(barX, barY, barX + barW, barY + 1, 0x30FFFFFF);
        ctx.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0x30FFFFFF);

        int fillW = (int)(ratio * (barW - 2));
        if (fillW > 0) {
            ctx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, color);
        }

        ctx.drawTextWithShadow(this.textRenderer, value, barX + barW + 4, y, C_TEXT);
    }

    @Override
    public void tick() {
        super.tick();

        ItemStack current = getVMStack();
        if (current.isEmpty()) {
            this.close();
            return;
        }

        int cx = (int) VortexManipulatorItem.getDestX(current);
        int cy = (int) VortexManipulatorItem.getDestY(current);
        int cz = (int) VortexManipulatorItem.getDestZ(current);
        String cdim = VortexManipulatorItem.getDestDim(current);

        if (cx != lastX && !this.xField.isFocused()) {
            this.xField.setText(String.valueOf(cx));
            lastX = cx;
        }
        if (cy != lastY && !this.yField.isFocused()) {
            this.yField.setText(String.valueOf(cy));
            lastY = cy;
        }
        if (cz != lastZ && !this.zField.isFocused()) {
            this.zField.setText(String.valueOf(cz));
            lastZ = cz;
        }

        lastDim = cdim;

        // ★ 每 tick 刷新传送按钮冷却状态
        updateGoButtonState(current);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static Text getDimensionText(String dimId) {
        if (dimId == null || dimId.isEmpty()) {
            return Text.literal("-");
        }
        try {
            Identifier id = new Identifier(dimId);
            String key = "dimension." + id.getNamespace() + "." + id.getPath();
            String translated = net.minecraft.client.resource.language.I18n.translate(key);

            if (translated.equals(key)) {
                return Text.literal(dimId);
            }
            return parseFormattingCodes(translated);
        } catch (Exception e) {
            return Text.literal(dimId);
        }
    }

    private static Text parseFormattingCodes(String input) {
        if (input == null || input.isEmpty()) return Text.empty();
        if (!input.contains("§")) return Text.literal(input);

        MutableText result = Text.empty();
        StringBuilder current = new StringBuilder();
        Formatting currentFormat = null;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§' && i + 1 < input.length()) {
                if (current.length() > 0) {
                    MutableText part = Text.literal(current.toString());
                    if (currentFormat != null) part.formatted(currentFormat);
                    result.append(part);
                    current.setLength(0);
                }
                char code = input.charAt(i + 1);
                currentFormat = Formatting.byCode(code);
                i++;
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            MutableText part = Text.literal(current.toString());
            if (currentFormat != null) part.formatted(currentFormat);
            result.append(part);
        }

        return result;
    }
}