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
import net.minecraft.util.Identifier;

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

    // 面板尺寸
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 220;

    public EyeOfHarmonyObeliskScreen(EyeOfHarmonyObeliskBlockEntity blockEntity) {
        super(Text.literal("Eye of Harmony Obelisk"));
        this.blockEntity = blockEntity;
        this.currentYOffset = Math.round(blockEntity.getYOffset());
        this.currentScale = blockEntity.getScale();
        this.currentEyeVisible = blockEntity.isEyeVisible();
        this.currentRedstoneMode = blockEntity.isRedstoneMode();
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int contentX = panelX + 20;
        int contentY = panelY + 35;

        // ===== 标题分隔线 =====
        // Y 偏移滑块
        double ySliderValue = (double) (currentYOffset - MIN_Y) / (MAX_Y - MIN_Y);
        SliderWidget ySlider = new SliderWidget(
                contentX, contentY,
                240, 18,
                Text.literal("Y Offset: " + currentYOffset),
                ySliderValue
        ) {
            @Override
            protected void updateMessage() {
                int actual = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                setMessage(Text.literal("§7Y Offset: §f" + actual));
            }

            @Override
            protected void applyValue() {
                currentYOffset = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                blockEntity.setYOffset(currentYOffset);
                syncToServer();
            }
        };

        // 体积缩放滑块
        double scaleSliderValue = (double) (currentScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        SliderWidget scaleSlider = new SliderWidget(
                contentX, contentY + 28,
                240, 18,
                Text.literal("Scale: " + String.format("%.1f", currentScale) + "x"),
                scaleSliderValue
        ) {
            @Override
            protected void updateMessage() {
                float actual = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                actual = Math.round(actual * 10.0f) / 10.0f;
                setMessage(Text.literal("§7Scale: §f" + String.format("%.1f", actual) + "x"));
            }

            @Override
            protected void applyValue() {
                currentScale = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                currentScale = Math.round(currentScale * 10.0f) / 10.0f;
                blockEntity.setScale(currentScale);
                syncToServer();
            }
        };

        // 和谐之眼显隐
        CheckboxWidget eyeToggle = new CheckboxWidget(
                contentX, contentY + 56,
                240, 18,
                Text.literal("Render Eye of Harmony"),
                currentEyeVisible
        ) {
            @Override
            public void onPress() {
                super.onPress();
                currentEyeVisible = this.isChecked();
                blockEntity.setEyeVisible(currentEyeVisible);
                syncToServer();
            }
        };

        // 红石模式
        CheckboxWidget redstoneToggle = new CheckboxWidget(
                contentX, contentY + 80,
                240, 18,
                Text.literal("Redstone Control"),
                currentRedstoneMode
        ) {
            @Override
            public void onPress() {
                super.onPress();
                currentRedstoneMode = this.isChecked();
                blockEntity.setRedstoneMode(currentRedstoneMode);
                syncToServer();
            }
        };

        // 完成按钮
        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                .dimensions(panelX + PANEL_WIDTH / 2 - 40, panelY + PANEL_HEIGHT - 30, 80, 20).build();

        addDrawableChild(ySlider);
        addDrawableChild(scaleSlider);
        addDrawableChild(eyeToggle);
        addDrawableChild(redstoneToggle);
        addDrawableChild(doneButton);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 不调用 renderBackground，自己画深色背景
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // 背景遮罩（半透明黑）
        context.fill(0, 0, this.width, this.height, 0xCC000000);

        // 主面板背景（深紫黑）
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A0A2E);

        // 面板边框（金色）
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        // 标题
        String title = "Eye of Harmony Obelisk";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawTextWithShadow(this.textRenderer, Text.literal(title).formatted(Formatting.GOLD),
                panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 10, 0xFFD700);

        // 标题下划线
        context.fill(panelX + 20, panelY + 24, panelX + PANEL_WIDTH - 20, panelY + 25, 0xFFFFD700);

        // 状态显示
        String status = blockEntity.isActive() ? "§aACTIVE" : "§cINACTIVE";
        int statusWidth = this.textRenderer.getWidth("Status: " + Formatting.strip(status));
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Status: " + status),
                panelX + (PANEL_WIDTH - statusWidth) / 2, panelY + PANEL_HEIGHT - 50, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}