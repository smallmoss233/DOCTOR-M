package doctor_m.client.gui;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import doctor_m.network.UpdateObeliskPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class EyeOfHarmonyObeliskScreen extends Screen {

    private final EyeOfHarmonyObeliskBlockEntity blockEntity;
    private int currentYOffset;      // ← 整数
    private float currentScale;     // ← 浮点，保留1位小数

    private static final int MIN_Y = -100;
    private static final int MAX_Y = 100;

    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 5.0f;

    public EyeOfHarmonyObeliskScreen(EyeOfHarmonyObeliskBlockEntity blockEntity) {
        super(Text.literal("Eye of Harmony Obelisk"));
        this.blockEntity = blockEntity;
        this.currentYOffset = Math.round(blockEntity.getYOffset());
        this.currentScale = blockEntity.getScale();
    }

    @Override
    protected void init() {
        // ===== Y 偏移滑块（整数） =====
        double ySliderValue = (double) (currentYOffset - MIN_Y) / (MAX_Y - MIN_Y);
        SliderWidget ySlider = new SliderWidget(
                this.width / 2 - 100, this.height / 2 - 40,
                200, 20,
                Text.literal(formatYLabel(currentYOffset)),
                ySliderValue
        ) {
            @Override
            protected void updateMessage() {
                int actual = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                setMessage(Text.literal(formatYLabel(actual)));
            }

            @Override
            protected void applyValue() {
                currentYOffset = Math.round((float) (MIN_Y + this.value * (MAX_Y - MIN_Y)));
                blockEntity.setYOffset(currentYOffset);
                syncToServer();
            }
        };

        // ===== 体积缩放滑块（浮点，0.1 ~ 5.0） =====
        double scaleSliderValue = (double) (currentScale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE);
        SliderWidget scaleSlider = new SliderWidget(
                this.width / 2 - 100, this.height / 2 - 10,
                200, 20,
                Text.literal(formatScaleLabel(currentScale)),
                scaleSliderValue
        ) {
            @Override
            protected void updateMessage() {
                float actual = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                actual = Math.round(actual * 10.0f) / 10.0f;
                setMessage(Text.literal(formatScaleLabel(actual)));
            }

            @Override
            protected void applyValue() {
                currentScale = (float) (MIN_SCALE + this.value * (MAX_SCALE - MIN_SCALE));
                currentScale = Math.round(currentScale * 10.0f) / 10.0f;
                blockEntity.setScale(currentScale);
                syncToServer();
            }
        };

        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height / 2 + 30, 100, 20).build();

        addDrawableChild(ySlider);
        addDrawableChild(scaleSlider);
        addDrawableChild(doneButton);
    }

    private void syncToServer() {
        ClientPlayNetworking.send(
                UpdateObeliskPacket.ID,
                UpdateObeliskPacket.createBuf(blockEntity.getPos(), currentYOffset, currentScale)
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static String formatYLabel(int value) {
        return "Y Offset: " + value;
    }

    private static String formatScaleLabel(float value) {
        return "Scale: " + String.format("%.1f", value) + "x";
    }
}