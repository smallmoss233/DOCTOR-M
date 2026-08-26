package doctor_m.client.gui;

import doctor_m.Item.data_item.KeytoTimeItem;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.handler.KeytoTime.KeytoTimePassive;
import doctor_m.network.KeytoTimeNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class KeytoTimePassiveScreen extends Screen {
    private final PlayerEntity player;
    private ButtonWidget godModeBtn, neutralBtn, slashBtn, saveTitleBtn;
    private TextFieldWidget titleField;

    public KeytoTimePassiveScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.key_to_time.passive.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        if (KeytoTimeCore.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }

        int cx = this.width / 2;
        int cy = this.height / 2;

        godModeBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.key_to_time.godmode_status", KeytoTimePassive.isGodMode(player)),
                btn -> send(0)
        ).position(cx - 80, cy - 48).size(160, 22).build());

        neutralBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.key_to_time.neutral_status", KeytoTimePassive.isNeutralMode(player)),
                btn -> send(1)
        ).position(cx - 80, cy - 16).size(160, 22).build());

        slashBtn = this.addDrawableChild(ButtonWidget.builder(
                toggleText("gui.doctor_m.key_to_time.slash_mode", KeytoTimePassive.isSlashMode(player))
                        .formatted(Formatting.DARK_RED),
                btn -> send(2)
        ).position(cx - 80, cy + 16).size(160, 22).build());

        ItemStack keyStack = KeytoTimeCore.getTimeKeyStack(player);
        String currentTitle = KeytoTimeItem.getTitle(keyStack);
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
        if (KeytoTimeCore.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }
        godModeBtn.setMessage(toggleText("gui.doctor_m.key_to_time.godmode_status", KeytoTimePassive.isGodMode(player)));
        neutralBtn.setMessage(toggleText("gui.doctor_m.key_to_time.neutral_status", KeytoTimePassive.isNeutralMode(player)));
        slashBtn.setMessage(toggleText("gui.doctor_m.key_to_time.slash_mode", KeytoTimePassive.isSlashMode(player)).formatted(Formatting.DARK_RED));
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
        ClientPlayNetworking.send(KeytoTimeNetwork.TOGGLE_PASSIVE, buf);
    }

    private void sendTitle() {
        String title = titleField.getText().trim();

        ItemStack keyStack = KeytoTimeCore.getTimeKeyStack(player);
        KeytoTimeItem.setTitle(keyStack, title);

        var buf = PacketByteBufs.create();
        buf.writeString(title, 64);
        ClientPlayNetworking.send(KeytoTimeNetwork.SET_TITLE, buf);
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
        int bgH = 218;
        int bgR = bgX + bgW;
        int bgB = bgY + bgH;

        ctx.fill(bgX, bgY, bgR, bgB, 0xF0100010);
        ctx.fill(bgX, bgY, bgR, bgY + 1, 0x505000FF);
        ctx.fill(bgX, bgB - 1, bgR, bgB, 0x505000FF);
        ctx.fill(bgX, bgY, bgX + 1, bgB, 0x5028007F);
        ctx.fill(bgR - 1, bgY, bgR, bgB, 0x5028007F);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, bgY + 8, 0xFFFFFF);
        ctx.fill(cx - 60, bgY + 22, cx + 60, bgY + 23, 0x30FFFFFF);

        ctx.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.title.label"),
                cx - 80, cy + 44, 0xAAAAAA);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}