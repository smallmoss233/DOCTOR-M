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

    public ATCLModificationSystemScreen(ATCLModificationSystemScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 400;
        this.backgroundHeight = 260;
        this.titleX = 9999;
        this.playerInventoryTitleY = 9999;
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        context.fillGradient(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x, y, this.backgroundWidth, this.backgroundHeight, COLOR_BORDER);

        for (int i = 0; i < 5; i++) {
            int sx = x + 61 + i * 65;
            int sy = y + 40;

            context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF888888);
            context.drawBorder(sx - 1, sy - 1, 18, 18, COLOR_BORDER);

            if (mouseX >= sx - 1 && mouseX <= sx + 17 && mouseY >= sy - 1 && mouseY <= sy + 17) {
                context.drawBorder(sx - 2, sy - 2, 20, 20, 0xFF00BFFF);
            }
        }

        ItemStack mainHand = ItemStack.EMPTY;
        if (this.client != null && this.client.player != null) {
            mainHand = this.client.player.getMainHandStack();
        }

        int iconCenterX = x + 85;
        int iconCenterY = y + 140;

        if (!mainHand.isEmpty()) {
            context.getMatrices().push();
            context.getMatrices().translate(iconCenterX, iconCenterY, 0);
            context.getMatrices().scale(3.5f, 3.5f, 1.0f);
            context.drawItem(mainHand, -8, -8);
            context.getMatrices().pop();

            String nameStr = mainHand.getName().getString();
            if (this.textRenderer.getWidth(nameStr) > 120) {
                nameStr = this.textRenderer.trimToWidth(nameStr, 115) + "...";
            }
            int nameWidth = this.textRenderer.getWidth(nameStr);
            context.drawText(this.textRenderer, nameStr, iconCenterX - nameWidth / 2, y + 180, 0xFF00BFFF, false);

            if (mainHand.getItem() instanceof SAR sar) {
                int maxE = sar.getMaxEnergy(mainHand);
                double extraSpeed = sar.getOrCreateSARNbt(mainHand).getDouble("atcl_extra_speed");
                int extraRegen = sar.getOrCreateSARNbt(mainHand).getInt("atcl_extra_regen");
                int extraDamage = sar.getOrCreateSARNbt(mainHand).getInt("atcl_extra_damage");

                Text energyText = Text.translatable("gui.doctor_m.atcl.max_energy", maxE);
                Text regenText = Text.translatable("gui.doctor_m.atcl.extra_regen", extraRegen * 20);
                Text damageText = Text.translatable("gui.doctor_m.atcl.extra_damage", extraDamage);
                Text speedText = Text.translatable("gui.doctor_m.atcl.extra_speed", String.format("%.1f", extraSpeed));

                drawCenteredScaledText(context, energyText, x + 70 + 65 * 0, y + 64, 0xFF009900);
                drawCenteredScaledText(context, regenText, x + 70 + 65 * 1, y + 64, 0xFFAA00AA);
                drawCenteredScaledText(context, damageText, x + 70 + 65 * 2, y + 64, 0xFFFF0000);
                drawCenteredScaledText(context, speedText, x + 70 + 65 * 3, y + 64, 0xFF0055AA);
            }
        } else {
            String noWeapon = Text.translatable("gui.doctor_m.atcl.no_weapon").getString();
            int nameWidth = this.textRenderer.getWidth(noWeapon);
            context.drawText(this.textRenderer, noWeapon, iconCenterX - nameWidth / 2, y + 180, 0xFFE50000, false);
        }

        int invX = x + 205;
        int invY = y + 100;

        context.fillGradient(invX - 10, invY - 10, invX + 172, invY + 88, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(invX - 10, invY - 10, 182, 98, COLOR_BORDER);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                int bx = invX + j * 18;
                int by = invY + i * 18;
                context.fill(bx, by, bx + 18, by + 18, 0xFF888888);
                context.drawBorder(bx, by, 18, 18, COLOR_BORDER);
            }
        }
        for (int j = 0; j < 9; j++) {
            int bx = invX + j * 18;
            int by = invY + 64;
            context.fill(bx, by, bx + 18, by + 18, 0xFF888888);
            context.drawBorder(bx, by, 18, 18, COLOR_BORDER);
        }
    }

    private void drawCenteredScaledText(DrawContext context, Text text, int centerX, int y, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 0);
        context.getMatrices().scale(0.75f, 0.75f, 1.0f);
        int width = this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, -width / 2, 0, color, false);
        context.getMatrices().pop();
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