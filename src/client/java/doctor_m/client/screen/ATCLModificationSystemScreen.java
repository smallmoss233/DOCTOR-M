package doctor_m.client.screen;

import doctor_m.menu.ATCLModificationSystemScreenHandler;
import doctor_m.module.creativity.creativity_data.SAR.SAR;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ATCLModificationSystemScreen extends HandledScreen<ATCLModificationSystemScreenHandler> {

    private static final int COLOR_BORDER = 0xFF111111;
    private static final int COLOR_HIGHLIGHT = 0xFF00BFFF;
    private static final int COLOR_TEXT = 0xFF000000;

    public ATCLModificationSystemScreen(ATCLModificationSystemScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 210;
        this.backgroundHeight = 200;
        this.titleX = 9999;
        this.playerInventoryTitleY = 9999;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // 主背景
        context.fillGradient(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x, y, this.backgroundWidth, this.backgroundHeight, COLOR_BORDER);

        // ================= 左侧终端面板 =================
        context.fillGradient(x + 6, y + 6, x + 94, y + 98, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x + 6, y + 6, 88, 92, COLOR_BORDER);
        context.fill(x + 8, y + 22, x + 92, y + 23, COLOR_BORDER);

        String titleStr = Text.translatable("gui.doctor_m.atcl.terminal").getString();
        context.drawText(this.textRenderer, titleStr, x + 12, y + 10, COLOR_HIGHLIGHT, false);

        ItemStack mainHand = ItemStack.EMPTY;
        if (this.client != null && this.client.player != null) {
            mainHand = this.client.player.getMainHandStack();
        }

        if (!mainHand.isEmpty()) {
            context.drawItem(mainHand, x + 42, y + 25);

            String nameStr = mainHand.getName().getString();
            if (this.textRenderer.getWidth(nameStr) > 110) {
                nameStr = this.textRenderer.trimToWidth(nameStr, 105) + "...";
            }
            context.getMatrices().push();
            context.getMatrices().translate(x + 50, y + 46, 0);
            context.getMatrices().scale(0.75f, 0.75f, 1.0f);
            int nameWidth = this.textRenderer.getWidth(nameStr);
            context.drawText(this.textRenderer, nameStr, -nameWidth / 2, 0, COLOR_TEXT, false);
            context.getMatrices().pop();

            if (mainHand.getItem() instanceof SAR sar) {
                int maxE = sar.getMaxEnergy(mainHand);
                String eStr = Text.translatable("gui.doctor_m.atcl.max_energy", maxE).getString();
                context.drawText(this.textRenderer, eStr, x + 50 - this.textRenderer.getWidth(eStr) / 2, y + 60, 0xFF009900, false);

                double extraSpeed = sar.getOrCreateSARNbt(mainHand).getDouble("atcl_extra_speed");
                String sStr = Text.translatable("gui.doctor_m.atcl.extra_speed", String.format("%.1f", extraSpeed)).getString();
                context.drawText(this.textRenderer, sStr, x + 50 - this.textRenderer.getWidth(sStr) / 2, y + 72, 0xFF0055AA, false);

                int extraRegen = sar.getOrCreateSARNbt(mainHand).getInt("atcl_extra_regen");
                String rStr = Text.translatable("gui.doctor_m.atcl.extra_regen", extraRegen * 20).getString();
                context.drawText(this.textRenderer, rStr, x + 50 - this.textRenderer.getWidth(rStr) / 2, y + 84, 0xFFAA00AA, false);
            }
        } else {
            String noWeapon = Text.translatable("gui.doctor_m.atcl.no_weapon").getString();
            context.drawText(this.textRenderer, noWeapon, x + 50 - this.textRenderer.getWidth(noWeapon) / 2, y + 50, 0xFFE50000, false);
        }

        // ================= 右侧改装槽位面板 =================
        context.fillGradient(x + 100, y + 6, x + 206, y + 98, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x + 100, y + 6, 106, 92, COLOR_BORDER);
        context.fill(x + 102, y + 22, x + 204, y + 23, COLOR_BORDER);

        String modTitle = Text.translatable("gui.doctor_m.atcl.modules").getString();
        context.drawText(this.textRenderer, modTitle, x + 106, y + 10, COLOR_HIGHLIGHT, false);

        // 绘制槽位底框 (修正：y + 40 对齐 Handler 里的槽位坐标)
        for (int i = 0; i < 5; i++) {
            int sx = x + 105 + i * 20;
            int sy = y + 40;
            context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF888888);
            context.drawBorder(sx - 1, sy - 1, 18, 18, COLOR_BORDER);
            if (mouseX >= sx - 1 && mouseX <= sx + 17 && mouseY >= sy - 1 && mouseY <= sy + 17) {
                context.drawBorder(sx - 2, sy - 2, 20, 20, COLOR_HIGHLIGHT);
            }
        }

        // ================= 红框区域：横向移动的方块 =================
        drawFloatingCubes(context, x, y);

        // ================= 底部背包区域 =================
        context.fillGradient(x + 4, y + 100, x + this.backgroundWidth - 4, y + 195, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x + 4, y + 100, this.backgroundWidth - 8, 95, COLOR_BORDER);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                int bx = x + 24 + j * 18;
                int by = y + 108 + i * 18;
                context.fill(bx - 1, by - 1, bx + 17, by + 17, 0xFF888888);
                context.drawBorder(bx - 1, by - 1, 18, 18, COLOR_BORDER);
            }
        }
        for (int j = 0; j < 9; j++) {
            int bx = x + 24 + j * 18;
            int by = y + 166;
            context.fill(bx - 1, by - 1, bx + 17, by + 17, 0xFF888888);
            context.drawBorder(bx - 1, by - 1, 18, 18, COLOR_BORDER);
        }
    }

    private void drawFloatingCubes(DrawContext context, int x, int y) {
        long currentTime = System.currentTimeMillis();
        float time = (currentTime % 3000L) / 3000.0F;

        float sin1 = (float) Math.sin(time * Math.PI * 2);
        float sin2 = (float) Math.sin(time * Math.PI * 2 + Math.PI);

        // 严格限制在右侧面板的 X 轴内部 (x+108 到 x+192 之间)
        int minX = x + 108;
        int maxX = x + 192;

        int x1 = minX + (int) ((maxX - minX) * (sin1 + 1) / 2);
        int x2 = minX + (int) ((maxX - minX) * (sin2 + 1) / 2);

        int centerY = y + 75;
        int size = 6;

        drawMiniCube(context, x1, centerY - 4, size);
        drawMiniCube(context, x2, centerY + 4, size);
    }

    private void drawMiniCube(DrawContext context, int x, int y, int size) {
        context.fillGradient(x, y, x + size, y + size, 0xFFB0B0B0, 0xFF707070);
        context.fill(x, y, x + size, y + 1, 0xFFE0E0E0);
        context.fill(x, y, x + 1, y + size, 0xFFE0E0E0);
        context.fill(x, y + size - 1, x + size, y + size, 0xFF333333);
        context.fill(x + size - 1, y, x + size, y + size, 0xFF333333);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}