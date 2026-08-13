package doctor_m.client.gui;

import doctor_m.client.util.id.PlayerTitleCache;
import doctor_m.handler.TimeKey.TimeKeyFunction;
import doctor_m.handler.TimeKey.TimeKeyPassive;
import doctor_m.network.TimeKeyNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TimeKeyPassiveScreen extends Screen {
    private final PlayerEntity player;
    private ButtonWidget godModeBtn, neutralBtn, slashBtn, saveTitleBtn;
    private TextFieldWidget titleField;

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

        // 三个功能按钮 —— 纵向间距从 ~6px 拉到 10px
        godModeBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.time_key.godmode_status", TimeKeyPassive.isGodMode(player)),
                btn -> send(0)
        ).position(cx - 80, cy - 48).size(160, 22).build());

        neutralBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.time_key.neutral_status", TimeKeyPassive.isNeutralMode(player)),
                btn -> send(1)
        ).position(cx - 80, cy - 16).size(160, 22).build());

        slashBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.time_key.slash_mode", TimeKeyPassive.isSlashMode(player))
                        .formatted(Formatting.DARK_RED),
                btn -> send(2)
        ).position(cx - 80, cy + 16).size(160, 22).build());

        // ===== 称号区域 —— 和按钮拉开 16px，内部也拉开 =====
        String currentTitle = PlayerTitleCache.getTitle(player.getUuid());
        if (currentTitle == null) currentTitle = "";

        titleField = new TextFieldWidget(this.textRenderer, cx - 80, cy + 54, 160, 20,
                Text.translatable("gui.doctor_m.title.hint"));
        titleField.setMaxLength(32);
        titleField.setText(currentTitle);
        titleField.setPlaceholder(Text.translatable("gui.doctor_m.title.placeholder"));
        this.addDrawableChild(titleField);

        saveTitleBtn = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.title.save"),
                btn -> sendTitle()
        ).position(cx - 80, cy + 84).size(160, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(cx - 40, cy + 114).size(80, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (TimeKeyFunction.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }
        godModeBtn.setMessage(toggleText("gui.doctor_m.time_key.godmode_status", TimeKeyPassive.isGodMode(player)));
        neutralBtn.setMessage(toggleText("gui.doctor_m.time_key.neutral_status", TimeKeyPassive.isNeutralMode(player)));
        slashBtn.setMessage(toggleText("gui.doctor_m.time_key.slash_mode", TimeKeyPassive.isSlashMode(player)).formatted(Formatting.DARK_RED));
        titleField.tick();
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

    private void sendTitle() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) title = "";

        var buf = PacketByteBufs.create();
        buf.writeString(title, 64);
        ClientPlayNetworking.send(new net.minecraft.util.Identifier("doctor_m", "set_title"), buf);
    }

    @Override
    public void renderBackground(DrawContext ctx) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        int bgX = cx - 100;
        int bgY = cy - 72;
        int bgW = 200;
        int bgH = 218;          // 加高，给底部留 12px 白边
        int bgR = bgX + bgW;
        int bgB = bgY + bgH;

        ctx.fill(bgX, bgY, bgR, bgB, 0xF0100010);
        ctx.fill(bgX, bgY, bgR, bgY + 1, 0x505000FF);
        ctx.fill(bgX, bgB - 1, bgR, bgB, 0x505000FF);
        ctx.fill(bgX, bgY, bgX + 1, bgB, 0x5028007F);
        ctx.fill(bgR - 1, bgY, bgR, bgB, 0x5028007F);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, bgY + 8, 0xFFFFFF);
        ctx.fill(cx - 60, bgY + 22, cx + 60, bgY + 23, 0x30FFFFFF);

        // 称号标签 —— 与输入框左边缘对齐，放在输入框上方 10px
        ctx.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.title.label"),
                cx - 80, cy + 44, 0xAAAAAA);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}