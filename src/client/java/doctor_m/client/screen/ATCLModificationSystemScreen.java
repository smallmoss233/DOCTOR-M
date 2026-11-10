package doctor_m.client.screen;

import doctor_m.menu.ATCLModificationSystemScreenHandler;
import doctor_m.module.creativity.creativity_data.SAR.SAR;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ATCLModificationSystemScreen extends HandledScreen<ATCLModificationSystemScreenHandler> {

    public ATCLModificationSystemScreen(ATCLModificationSystemScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 200;
        this.backgroundHeight = 200;
        this.titleX = 9999;
        this.playerInventoryTitleY = 9999;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // 主背景板
        context.fill(x - 2, y - 2, x + this.backgroundWidth + 2, y + this.backgroundHeight + 2, 0xFF000000);
        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFF2D2D2D);

        // ================= 左侧终端面板 =================
        context.fill(x + 6, y + 6, x + 94, y + 98, 0xFF404040);
        context.drawBorder(x + 6, y + 6, 88, 92, 0xFF000000);
        context.drawText(textRenderer, "A.T.C.L TERMINAL", x + 10, y + 10, 0xFF00FFFF, false);
        context.fill(x + 8, y + 22, x + 92, y + 23, 0xFF000000);

        ItemStack mainHand = ItemStack.EMPTY;
        if (this.client != null && this.client.player != null) {
            mainHand = this.client.player.getMainHandStack();
        }

        if (!mainHand.isEmpty()) {
            context.drawItem(mainHand, x + 42, y + 30);

            // 处理名字过长截断
            String nameStr = mainHand.getName().getString();
            if (textRenderer.getWidth(nameStr) > 80) {
                nameStr = textRenderer.trimToWidth(nameStr, 74) + "...";
            }
            context.drawText(textRenderer, nameStr, x + 50 - textRenderer.getWidth(nameStr) / 2, y + 55, 0xFFFFFF, false);

            if (mainHand.getItem() instanceof SAR sar) {
                int maxE = sar.getMaxEnergy(mainHand);
                Text energyText = Text.literal("能量上限: " + maxE).formatted(Formatting.GREEN);
                context.drawText(textRenderer, energyText, x + 50 - textRenderer.getWidth(energyText) / 2, y + 75, 0xFFFFFF, false);
            }
        }

        // ================= 右侧改装槽位面板 =================
        context.fill(x + 100, y + 6, x + 194, y + 98, 0xFF404040);
        context.drawBorder(x + 100, y + 6, 94, 92, 0xFF000000);
        context.drawText(textRenderer, "MODULES", x + 104, y + 10, 0xFF00FFFF, false);
        context.fill(x + 102, y + 22, x + 192, y + 23, 0xFF000000);

        // 主槽位底框 (高亮)
        context.fill(x + 136, y + 23, x + 156, y + 43, 0xFF222222);
        context.drawBorder(x + 136, y + 23, 20, 20, 0xFF00FFFF);

        // 副槽位底框 (2x2)
        for (int i = 1; i < 5; i++) {
            int sx = x + 121 + ((i - 1) % 2) * 36;
            int sy = y + 57 + ((i - 1) / 2) * 22;
            context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF222222);
            context.drawBorder(sx - 1, sy - 1, 18, 18, 0xFF555555);
        }

        // ================= 底部背包区域 =================
        context.fill(x + 4, y + 100, x + this.backgroundWidth - 4, y + 195, 0xFF3A3A3A);
        context.drawBorder(x + 4, y + 100, this.backgroundWidth - 8, 95, 0xFF000000);
        context.drawText(textRenderer, Text.translatable("container.inventory").formatted(Formatting.GRAY), x + 8, y + 101, 0xFFFFFF, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}