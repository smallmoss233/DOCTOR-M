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

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 220;

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
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int contentX = panelX + 20;
        int contentY = panelY + 35;

        double ySliderValue = (double) (currentYOffset - MIN_Y) / (MAX_Y - MIN_Y);
        SliderWidget ySlider = new SliderWidget(
                contentX, contentY, 240, 18,
                Text.translatable("gui.doctor_m.obelisk.y_offset", currentYOffset),
                ySliderValue
        ) {
            @Override
            protected void updateMessage() {
                int actual = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                setMessage(Text.translatable("gui.doctor_m.obelisk.y_offset", actual));
            }
            @Override
            protected void applyValue() {
                currentYOffset = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                blockEntity.setYOffset(currentYOffset);
                syncToServer();
            }
        };

        double scaleSliderValue = (double) (currentScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        SliderWidget scaleSlider = new SliderWidget(
                contentX, contentY + 28, 240, 18,
                Text.translatable("gui.doctor_m.obelisk.scale", String.format("%.1f", currentScale)),
                scaleSliderValue
        ) {
            @Override
            protected void updateMessage() {
                float actual = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                actual = Math.round(actual * 10.0f) / 10.0f;
                setMessage(Text.translatable("gui.doctor_m.obelisk.scale", String.format("%.1f", actual)));
            }
            @Override
            protected void applyValue() {
                currentScale = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                currentScale = Math.round(currentScale * 10.0f) / 10.0f;
                blockEntity.setScale(currentScale);
                syncToServer();
            }
        };

        CheckboxWidget eyeToggle = new CheckboxWidget(
                contentX, contentY + 56, 240, 18,
                Text.translatable("gui.doctor_m.obelisk.render_eye"),
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

        CheckboxWidget redstoneToggle = new CheckboxWidget(
                contentX, contentY + 80, 240, 18,
                Text.translatable("gui.doctor_m.obelisk.redstone_control"),
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

        ButtonWidget doneButton = ButtonWidget.builder(
                Text.translatable("gui.doctor_m.obelisk.done"),
                button -> this.close()
        ).dimensions(panelX + PANEL_WIDTH / 2 - 40, panelY + PANEL_HEIGHT - 30, 80, 20).build();

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
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        context.fill(0, 0, this.width, this.height, 0xCC000000);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1A0A2E);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD700);

        String title = Text.translatable("gui.doctor_m.obelisk.title").getString();
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(title).formatted(Formatting.GOLD),
                panelX + (PANEL_WIDTH - titleWidth) / 2, panelY + 10, 0xFFD700);

        context.fill(panelX + 20, panelY + 24, panelX + PANEL_WIDTH - 20, panelY + 25, 0xFFFFD700);

        String statusKey = blockEntity.isActive()
                ? "gui.doctor_m.obelisk.active"
                : "gui.doctor_m.obelisk.inactive";
        Text statusText = Text.translatable("gui.doctor_m.obelisk.status",
                Text.translatable(statusKey));
        int statusWidth = this.textRenderer.getWidth(statusText);
        context.drawTextWithShadow(this.textRenderer, statusText,
                panelX + (PANEL_WIDTH - statusWidth) / 2, panelY + PANEL_HEIGHT - 50, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}