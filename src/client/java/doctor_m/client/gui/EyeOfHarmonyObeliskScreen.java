package doctor_m.client.gui;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import doctor_m.client.ObeliskClientCache;
import doctor_m.network.UpdateObeliskPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class EyeOfHarmonyObeliskScreen extends Screen {

    private final EyeOfHarmonyObeliskBlockEntity blockEntity;
    private float currentYOffset;

    private SliderWidget yOffsetSlider;
    private ButtonWidget doneButton;

    private static final float MIN_Y = 10f;
    private static final float MAX_Y = 100f;

    public EyeOfHarmonyObeliskScreen(EyeOfHarmonyObeliskBlockEntity blockEntity) {
        super(Text.literal("Eye of Harmony Obelisk"));
        this.blockEntity = blockEntity;
        this.currentYOffset = blockEntity.getYOffset();
    }

    @Override
    protected void init() {
        double sliderValue = (double) (currentYOffset - MIN_Y) / (MAX_Y - MIN_Y);

        yOffsetSlider = new SliderWidget(
                this.width / 2 - 100,
                this.height / 2 - 20,
                200, 20,
                Text.literal(formatLabel(currentYOffset)),
                sliderValue
        ) {
            @Override
            protected void updateMessage() {
                double actual = MIN_Y + this.value * (MAX_Y - MIN_Y);
                setMessage(Text.literal(formatLabel((float) actual)));
            }

            @Override
            protected void applyValue() {
                currentYOffset = (float) (MIN_Y + this.value * (MAX_Y - MIN_Y));

                // ← 关键 1：客户端预测，本地 BlockEntity 立即更新，渲染器实时响应
                blockEntity.setYOffset(currentYOffset);
                ObeliskClientCache.update(blockEntity.getPos(), currentYOffset);

                // ← 关键 2：同步到服务器，触发 markDirty + NBT 持久化 + 广播给其他玩家
                ClientPlayNetworking.send(
                        UpdateObeliskPacket.ID,
                        UpdateObeliskPacket.createBuf(blockEntity.getPos(), currentYOffset)
                );
            }
        };

        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                .dimensions(this.width / 2 - 50, this.height / 2 + 20, 100, 20).build();

        addDrawableChild(yOffsetSlider);
        addDrawableChild(doneButton);
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

    private static String formatLabel(float value) {
        return "Y Offset: " + String.format("%.1f", value);
    }
}