package doctor_m.client.render;

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
import net.minecraft.util.Identifier;

import doctor_m.Item.data_itme.VortexManipulatorItem;
import doctor_m.Item.items;
import doctor_m.network.VMNetwork;

public class VortexManipulatorScreen extends Screen {
    private final PlayerEntity player;
    private TextFieldWidget xField, yField, zField;

    // 用 int 避免浮点抖动，但初始值要设成不可能出现的值
    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private String lastDim = null;

    public VortexManipulatorScreen(PlayerEntity player, ItemStack stack) {
        super(Text.translatable("gui.doctor_m.vm.title"));
        this.player = player;
    }

    /** 关键修复：每次实时从玩家手中获取，不缓存 ItemStack 引用 */
    private ItemStack getVMStack() {
        ItemStack main = player.getMainHandStack();
        if (main.isOf(items.VORTEX_MANIPULATOR)) return main;
        ItemStack off = player.getOffHandStack();
        if (off.isOf(items.VORTEX_MANIPULATOR)) return off;
        return ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftX = centerX - 130;
        int rightX = centerX + 10;
        int startY = centerY - 50;

        ItemStack stack = getVMStack();
        if (stack.isEmpty()) {
            this.close();
            return;
        }

        this.zField = new TextFieldWidget(this.textRenderer, leftX, startY, 110, 18, Text.literal("Z"));
        this.zField.setText(String.valueOf((int) VortexManipulatorItem.getDestZ(stack)));
        this.addDrawableChild(this.zField);

        this.yField = new TextFieldWidget(this.textRenderer, leftX, startY + 26, 110, 18, Text.literal("Y"));
        this.yField.setText(String.valueOf((int) VortexManipulatorItem.getDestY(stack)));
        this.addDrawableChild(this.yField);

        this.xField = new TextFieldWidget(this.textRenderer, leftX, startY + 52, 110, 18, Text.literal("X"));
        this.xField.setText(String.valueOf((int) VortexManipulatorItem.getDestX(stack)));
        this.addDrawableChild(this.xField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> {
            var buf = PacketByteBufs.create();
            buf.writeBoolean(true);
            ClientPlayNetworking.send(VMNetwork.CYCLE_DIM, buf);
        }).position(rightX, startY).size(20, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> {
            var buf = PacketByteBufs.create();
            buf.writeBoolean(false);
            ClientPlayNetworking.send(VMNetwork.CYCLE_DIM, buf);
        }).position(rightX + 128, startY).size(20, 18).build());

        int btnY = startY + 28;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.set_current"),
                btn -> ClientPlayNetworking.send(VMNetwork.SET_CURRENT_DEST, PacketByteBufs.empty())
        ).position(rightX, btnY).size(148, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.set_prev"),
                btn -> ClientPlayNetworking.send(VMNetwork.SET_PREV_DEST, PacketByteBufs.empty())
        ).position(rightX, btnY + 24).size(148, 20).build());

        int bottomY = centerY + 60;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(centerX - 100, bottomY).size(90, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.go").formatted(Formatting.GREEN),
                btn -> attemptTeleport()
        ).position(centerX + 10, bottomY).size(90, 20).build());

        syncLastValues(stack);
    }

    private void syncLastValues(ItemStack stack) {
        lastX = (int) VortexManipulatorItem.getDestX(stack);
        lastY = (int) VortexManipulatorItem.getDestY(stack);
        lastZ = (int) VortexManipulatorItem.getDestZ(stack);
        lastDim = VortexManipulatorItem.getDestDim(stack);
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
            ClientPlayNetworking.send(VMNetwork.TELEPORT, buf);
            this.close();
        } catch (NumberFormatException e) {
            // 可选：播放错误音效
        }
    }

    @Override
    public void tick() {
        super.tick();

        ItemStack current = getVMStack();
        if (current.isEmpty()) {
            this.close();
            return;
        }

        int cx = (int) VortexManipulatorItem.getDestX(current);
        int cy = (int) VortexManipulatorItem.getDestY(current);
        int cz = (int) VortexManipulatorItem.getDestZ(current);
        String cdim = VortexManipulatorItem.getDestDim(current);

        // 关键修复：只要数值变了（且输入框没焦点），就实时刷新显示
        if (cx != lastX && !this.xField.isFocused()) {
            this.xField.setText(String.valueOf(cx));
            lastX = cx;
        }
        if (cy != lastY && !this.yField.isFocused()) {
            this.yField.setText(String.valueOf(cy));
            lastY = cy;
        }
        if (cz != lastZ && !this.zField.isFocused()) {
            this.zField.setText(String.valueOf(cz));
            lastZ = cz;
        }

        lastDim = cdim;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftX = centerX - 130;
        int rightX = centerX + 10;
        int startY = centerY - 50;

        ItemStack stack = getVMStack();
        if (stack.isEmpty()) return;

        int fuel = VortexManipulatorItem.getFuel(stack);
        int overheat = VortexManipulatorItem.getOverheat(stack);
        boolean broken = VortexManipulatorItem.isBroken(stack);
        String dimId = VortexManipulatorItem.getDestDim(stack);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, centerY - 78, 0xFFFFFF);

        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.z"), leftX - 15, startY + 4, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.y"), leftX - 15, startY + 30, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.doctor_m.vm.label.x"), leftX - 15, startY + 56, 0xAAAAAA);

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.vm.dimension"),
                rightX, startY - 14, 0xAAAAAA);

        Text dimText = getDimensionText(dimId);
        int dimTextWidth = this.textRenderer.getWidth(dimText);
        int dimAreaCenter = rightX + 74;
        context.drawTextWithShadow(this.textRenderer, dimText,
                dimAreaCenter - (dimTextWidth / 2), startY + 4, 0xFFFFFF);

        Text fuelText = Text.translatable("gui.doctor_m.vm.fuel_label", fuel, VortexManipulatorItem.MAX_FUEL)
                .formatted(fuel < 100 ? Formatting.RED : Formatting.GREEN);
        Text ohText = Text.translatable("gui.doctor_m.vm.heat_label", overheat)
                .formatted(overheat > 80 ? Formatting.RED : Formatting.YELLOW);

        context.drawTextWithShadow(this.textRenderer, fuelText, leftX, centerY + 28, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, ohText, leftX, centerY + 40, 0xFFFFFF);

        if (broken) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.doctor_m.vm.broken_label").formatted(Formatting.DARK_RED, Formatting.BOLD),
                    centerX, centerY + 52, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static Text getDimensionText(String dimId) {
        if (dimId == null || dimId.isEmpty()) {
            return Text.literal("-");
        }
        try {
            Identifier id = new Identifier(dimId);
            String key = "dimension." + id.getNamespace() + "." + id.getPath();
            String translated = net.minecraft.client.resource.language.I18n.translate(key);

            if (translated.equals(key)) {
                return Text.literal(dimId);
            }
            return parseFormattingCodes(translated);
        } catch (Exception e) {
            return Text.literal(dimId);
        }
    }

    private static Text parseFormattingCodes(String input) {
        if (input == null || input.isEmpty()) return Text.empty();
        if (!input.contains("§")) return Text.literal(input);

        MutableText result = Text.empty();
        StringBuilder current = new StringBuilder();
        Formatting currentFormat = null;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§' && i + 1 < input.length()) {
                if (current.length() > 0) {
                    MutableText part = Text.literal(current.toString());
                    if (currentFormat != null) part.formatted(currentFormat);
                    result.append(part);
                    current.setLength(0);
                }
                char code = input.charAt(i + 1);
                currentFormat = Formatting.byCode(code);
                i++;
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            MutableText part = Text.literal(current.toString());
            if (currentFormat != null) part.formatted(currentFormat);
            result.append(part);
        }

        return result;
    }
}