package doctor_m.client.gui;

import doctor_m.handler.TimeKey.TimeKeyFunction;
import doctor_m.handler.TimeKey.TimeKeyPassive;
import doctor_m.network.TimeKeyNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TimeKeyPassiveScreen extends Screen {
    private final PlayerEntity player;
    private ButtonWidget godModeBtn, neutralBtn, slashBtn;

    public TimeKeyPassiveScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.time_key.passive.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        if (TimeKeyFunction.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }

        int cx = this.width / 2;
        int cy = this.height / 2;

        godModeBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.time_key.godmode_status", TimeKeyPassive.isGodMode(player)),
                btn -> send(0)
        ).position(cx - 80, cy - 32).size(160, 22).build());

        neutralBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.time_key.neutral_status", TimeKeyPassive.isNeutralMode(player)),
                btn -> send(1)
        ).position(cx - 80, cy - 6).size(160, 22).build());

        slashBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.time_key.slash_mode", TimeKeyPassive.isSlashMode(player))
                        .formatted(Formatting.DARK_RED),
                btn -> send(2)
        ).position(cx - 80, cy + 20).size(160, 22).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(cx - 40, cy + 56).size(80, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (TimeKeyFunction.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }
        // 实时刷新按钮状态，点了立刻变
        godModeBtn.setMessage(toggleText("gui.doctor_m.time_key.godmode_status", TimeKeyPassive.isGodMode(player)));
        neutralBtn.setMessage(toggleText("gui.doctor_m.time_key.neutral_status", TimeKeyPassive.isNeutralMode(player)));
        slashBtn.setMessage(toggleText("gui.doctor_m.time_key.slash_mode", TimeKeyPassive.isSlashMode(player)).formatted(Formatting.DARK_RED));
    }

    private MutableText toggleText(String key, boolean enabled) {
        Text state = enabled
                ? Text.translatable("key.doctor_m.mode.on").formatted(Formatting.GREEN)
                : Text.translatable("key.doctor_m.mode.off").formatted(Formatting.RED);
        return Text.translatable(key, state);
    }

    private void send(int featureId) {
        var buf = PacketByteBufs.create();
        buf.writeInt(featureId);
        ClientPlayNetworking.send(TimeKeyNetwork.TOGGLE_PASSIVE, buf);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 65, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}