package doctor_m.client.gui;

import doctor_m.network.TimeKeyTeleportNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TimeKeyTeleportScreen extends Screen {
    private final PlayerEntity player;
    private TextFieldWidget xField, yField, zField;

    private List<String> availableDims = new ArrayList<>();
    private int dimIndex = 0;
    private boolean dimsLoaded = false;

    public TimeKeyTeleportScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.time_key.teleport.title"));
        this.player = player;
        ClientPlayNetworking.send(TimeKeyTeleportNetwork.REQUEST_DIMS, PacketByteBufs.empty());
    }

    public void onDimensionsReceived(List<String> dims) {
        this.availableDims = dims;
        this.dimsLoaded = true;
        String current = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
        int idx = availableDims.indexOf(current);
        this.dimIndex = idx >= 0 ? idx : 0;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftX = centerX - 130;
        int rightX = centerX + 10;
        int startY = centerY - 50;

        this.zField = new TextFieldWidget(this.textRenderer, leftX, startY, 110, 18, Text.literal("Z"));
        this.zField.setText(String.valueOf((int) player.getZ()));
        this.addDrawableChild(this.zField);

        this.yField = new TextFieldWidget(this.textRenderer, leftX, startY + 26, 110, 18, Text.literal("Y"));
        this.yField.setText(String.valueOf((int) player.getY()));
        this.addDrawableChild(this.yField);

        this.xField = new TextFieldWidget(this.textRenderer, leftX, startY + 52, 110, 18, Text.literal("X"));
        this.xField.setText(String.valueOf((int) player.getX()));
        this.addDrawableChild(this.xField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> cycleDim(-1))
                .position(rightX, startY).size(20, 18).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> cycleDim(1))
                .position(rightX + 128, startY).size(20, 18).build());

        int btnY = startY + 28;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.go").formatted(Formatting.GREEN),
                btn -> attemptTeleport()
        ).position(rightX, btnY).size(148, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(rightX, btnY + 24).size(148, 20).build());
    }

    private void cycleDim(int dir) {
        if (availableDims.isEmpty()) return;
        dimIndex = (dimIndex + dir + availableDims.size()) % availableDims.size();
    }

    private void attemptTeleport() {
        if (availableDims.isEmpty()) return;
        try {
            double x = Double.parseDouble(this.xField.getText());
            double y = Double.parseDouble(this.yField.getText());
            double z = Double.parseDouble(this.zField.getText());

            var buf = PacketByteBufs.create();
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeString(availableDims.get(dimIndex));
            ClientPlayNetworking.send(TimeKeyTeleportNetwork.TELEPORT, buf);
            this.close();
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void renderBackground(DrawContext context) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // ===== Tooltip 风格背景框（比 VM 界面稍大） =====
        int bgX = centerX - 155;
        int bgY = centerY - 90;
        int bgW = 320;
        int bgH = 185;
        int bgR = bgX + bgW;
        int bgB = bgY + bgH;

        context.fill(bgX, bgY, bgR, bgB, 0xF0100010);
        context.fill(bgX, bgY, bgR, bgY + 1, 0x505000FF);
        context.fill(bgX, bgB - 1, bgR, bgB, 0x505000FF);
        context.fill(bgX, bgY, bgX + 1, bgB, 0x5028007F);
        context.fill(bgR - 1, bgY, bgR, bgB, 0x5028007F);
        // =================================================

        int leftX = centerX - 130;
        int rightX = centerX + 10;
        int startY = centerY - 50;

        // 标题在框内顶部
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, bgY + 8, 0xFFFFFF);
        // 分割线
        context.fill(centerX - 100, bgY + 22, centerX + 100, bgY + 23, 0x30FFFFFF);

        // XYZ 标签
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.z"), leftX - 15, startY + 4, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.y"), leftX - 15, startY + 30, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.x"), leftX - 15, startY + 56, 0xAAAAAA);

        // 维度标签
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.vm.dimension"),
                rightX, startY - 14, 0xAAAAAA);

        Text dimText;
        if (availableDims.isEmpty()) {
            dimText = Text.literal("-").formatted(Formatting.RED);
        } else {
            dimText = getDimensionText(availableDims.get(dimIndex));
        }

        int dimTextWidth = this.textRenderer.getWidth(dimText);
        int dimAreaCenter = rightX + 74;
        context.drawTextWithShadow(this.textRenderer, dimText,
                dimAreaCenter - (dimTextWidth / 2), startY + 4, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static Text getDimensionText(String dimId) {
        if (dimId == null || dimId.isEmpty()) return Text.literal("-");
        try {
            Identifier id = new Identifier(dimId);
            String key = "dimension." + id.getNamespace() + "." + id.getPath();
            String translated = net.minecraft.client.resource.language.I18n.translate(key);
            if (translated.equals(key)) return Text.literal(dimId);
            return Text.literal(translated);
        } catch (Exception e) {
            return Text.literal(dimId);
        }
    }
}