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

import java.util.Arrays;
import java.util.List;

public class TimeKeyTeleportScreen extends Screen {
    private final PlayerEntity player;
    private TextFieldWidget xField, yField, zField;

    // ★ 和 VM 完全一致的维度池（包括被 VM 锁定的维度）
    private static final List<String> ALL_DIMENSIONS = Arrays.asList(
            "minecraft:overworld",
            "minecraft:the_nether",
            "minecraft:the_end",
            "doctor_m:trenzalore",
            "doctor_m:titan"
            // 如果 VM 还有其他维度，往这里加，KTT 都能进
    );

    private int dimIndex = 0;

    public TimeKeyTeleportScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.time_key.teleport.title"));
        this.player = player;
        // 默认选中当前所在维度
        String current = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
        dimIndex = ALL_DIMENSIONS.indexOf(current);
        if (dimIndex == -1) dimIndex = 0;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftX = centerX - 130;
        int rightX = centerX + 10;
        int startY = centerY - 50;

        // 每次打开自动填入当前位置
        this.zField = new TextFieldWidget(this.textRenderer, leftX, startY, 110, 18, Text.literal("Z"));
        this.zField.setText(String.valueOf((int) player.getZ()));
        this.addDrawableChild(this.zField);

        this.yField = new TextFieldWidget(this.textRenderer, leftX, startY + 26, 110, 18, Text.literal("Y"));
        this.yField.setText(String.valueOf((int) player.getY()));
        this.addDrawableChild(this.yField);

        this.xField = new TextFieldWidget(this.textRenderer, leftX, startY + 52, 110, 18, Text.literal("X"));
        this.xField.setText(String.valueOf((int) player.getX()));
        this.addDrawableChild(this.xField);

        // 维度切换（遍历完整列表，不跳过锁定维度）
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> cycleDim(-1))
                .position(rightX, startY).size(20, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> cycleDim(1))
                .position(rightX + 128, startY).size(20, 18).build());

        int btnY = startY + 28;

        // 前往（无任何限制，无视 VM 锁定）
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.go").formatted(Formatting.GREEN),
                btn -> attemptTeleport()
        ).position(rightX, btnY).size(148, 20).build());

        // 关闭
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(rightX, btnY + 24).size(148, 20).build());
    }

    private void cycleDim(int dir) {
        dimIndex = (dimIndex + dir + ALL_DIMENSIONS.size()) % ALL_DIMENSIONS.size();
    }

    private void attemptTeleport() {
        try {
            double x = Double.parseDouble(this.xField.getText());
            double y = Double.parseDouble(this.yField.getText());
            double z = Double.parseDouble(this.zField.getText());

            var buf = PacketByteBufs.create();
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeString(ALL_DIMENSIONS.get(dimIndex));
            ClientPlayNetworking.send(TimeKeyTeleportNetwork.TELEPORT, buf);
            this.close();
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftX = centerX - 130;
        int rightX = centerX + 10;
        int startY = centerY - 50;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, centerY - 78, 0xFFFFFF);

        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.z"), leftX - 15, startY + 4, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.y"), leftX - 15, startY + 30, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.x"), leftX - 15, startY + 56, 0xAAAAAA);

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.vm.dimension"),
                rightX, startY - 14, 0xAAAAAA);

        Text dimText = getDimensionText(ALL_DIMENSIONS.get(dimIndex));
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