package doctor_m.client.screen;

import doctor_m.menu.SARGUI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SarWorkbenchScreen extends HandledScreen<SARGUI> {
    private static final Identifier TEXTURE = new Identifier("doctor_m", "textures/gui/sar_workbench.png");

    public SarWorkbenchScreen(SARGUI handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // 设置背景图的宽高（标准尺寸通常是 176x166，或者 176x222，取决于你画的图）
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // 居中显示标题文字
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6; // 标题在背景图上的 Y 轴偏移
    }

    // 1.20.1 渲染背景的方法
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // 计算 GUI 在屏幕上的居中位置
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // 画背景图：context.drawTexture(贴图, x, y, u, v, 宽, 高)
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染半透明黑色背景（让后面的游戏世界变暗）
        renderBackground(context);

        // 调用父类，负责绘制物品槽和物品
        super.render(context, mouseX, mouseY, delta);

        // 绘制鼠标悬停在物品上时的 Tooltip（提示信息）
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}