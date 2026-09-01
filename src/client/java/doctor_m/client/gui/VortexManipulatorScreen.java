package doctor_m.client.gui;

import doctor_m.Item.data_item.VortexManipulatorItem;
import doctor_m.Item.items;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.network.VMNetwork;
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

public class VortexManipulatorScreen extends Screen {
    private static final ModConfig CONFIG = ConfigManager.getConfig();

    private final PlayerEntity player;
    private TextFieldWidget xField, yField, zField;

    private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
    private String lastDim = null;

    public VortexManipulatorScreen(PlayerEntity player, ItemStack stack) {
        super(Text.translatable("gui.doctor_m.vm.title"));
        this.player = player;
    }

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

        int bgW = 340;
        int bgH = 185;
        int bgX = centerX - bgW / 2;
        int padding = 16;

        int labelWidth = 14;
        int labelGap = 8;
        int fieldWidth = 110;
        int leftX = bgX + padding + labelWidth + labelGap;

        int gap = 10;
        int dividerX = centerX;
        int rightX = centerX + gap / 2;
        int rightWidth = 148;

        int startY = centerY - 50;

        int bottomBtnW = 140;
        int bottomLeftX = centerX - 145;
        int bottomRightX = centerX + 5;

        ItemStack stack = getVMStack();
        if (stack.isEmpty()) {
            this.close();
            return;
        }

        this.zField = new TextFieldWidget(this.textRenderer, leftX, startY, fieldWidth, 18, Text.literal("Z"));
        this.zField.setText(String.valueOf((int) VortexManipulatorItem.getDestZ(stack)));
        this.addDrawableChild(this.zField);

        this.yField = new TextFieldWidget(this.textRenderer, leftX, startY + 26, fieldWidth, 18, Text.literal("Y"));
        this.yField.setText(String.valueOf((int) VortexManipulatorItem.getDestY(stack)));
        this.addDrawableChild(this.yField);

        this.xField = new TextFieldWidget(this.textRenderer, leftX, startY + 52, fieldWidth, 18, Text.literal("X"));
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
        }).position(rightX + rightWidth - 20, startY).size(20, 18).build());

        int btnY = startY + 28;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.set_current"),
                btn -> ClientPlayNetworking.send(VMNetwork.SET_CURRENT_DEST, PacketByteBufs.empty())
        ).position(rightX, btnY).size(rightWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.set_prev"),
                btn -> ClientPlayNetworking.send(VMNetwork.SET_PREV_DEST, PacketByteBufs.empty())
        ).position(rightX, btnY + 24).size(rightWidth, 20).build());

        int bottomY = centerY + 60;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(bottomLeftX, bottomY).size(bottomBtnW, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.go").formatted(Formatting.GREEN),
                btn -> attemptTeleport()
        ).position(bottomRightX, bottomY).size(bottomBtnW, 20).build());

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
            // 忽略
        }
    }

    @Override
    public void renderBackground(DrawContext context) {
        // 禁用原版遮罩
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int bgW = 340;
        int bgH = 185;
        int bgX = centerX - bgW / 2;
        int bgY = centerY - bgH / 2;
        int bgR = bgX + bgW;
        int bgB = bgY + bgH;
        int padding = 16;

        int labelWidth = 14;
        int labelGap = 8;
        int fieldWidth = 110;
        int leftX = bgX + padding + labelWidth + labelGap;
        int labelRightX = leftX - labelGap;

        int gap = 10;
        int dividerX = centerX;
        int rightX = centerX + gap / 2;
        int rightWidth = 148;
        int startY = centerY - 50;

        int bottomBtnW = 140;
        int bottomLeftX = centerX - 145;
        int bottomRightX = centerX + 5;

        ItemStack stack = getVMStack();
        if (stack.isEmpty()) return;

        int fuel = VortexManipulatorItem.getFuel(stack);
        int overheat = VortexManipulatorItem.getOverheat(stack);
        boolean broken = VortexManipulatorItem.isBroken(stack);
        String dimId = VortexManipulatorItem.getDestDim(stack);

        context.fill(bgX, bgY, bgR, bgB, 0xF0100010);
        context.fill(bgX, bgY, bgR, bgY + 1, 0x505000FF);
        context.fill(bgX, bgB - 1, bgR, bgB, 0x505000FF);
        context.fill(bgX, bgY, bgX + 1, bgB, 0x5028007F);
        context.fill(bgR - 1, bgY, bgR, bgB, 0x5028007F);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, bgY + 8, 0xFFFFFF);
        context.fill(bgX + padding, bgY + 20, bgR - padding, bgY + 21, 0x505000FF);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gui.doctor_m.vm.dimension"),
                rightX + rightWidth / 2, startY - 14, 0xAAAAAA);

        Text dimText = getDimensionText(dimId);
        int dimTextWidth = this.textRenderer.getWidth(dimText);
        int dimAreaCenter = rightX + rightWidth / 2;
        context.drawTextWithShadow(this.textRenderer, dimText,
                dimAreaCenter - (dimTextWidth / 2), startY + 4, 0xFFFFFF);

        String[] labelKeys = {
                "gui.doctor_m.vm.label.z",
                "gui.doctor_m.vm.label.y",
                "gui.doctor_m.vm.label.x"
        };
        for (int i = 0; i < 3; i++) {
            Text label = Text.translatable(labelKeys[i]);
            int lw = this.textRenderer.getWidth(label);
            int ly = startY + i * 26 + 5;
            context.drawTextWithShadow(this.textRenderer, label,
                    labelRightX - lw, ly, 0xAAAAAA);
        }

        int sepY = startY + 75;
        context.fill(dividerX, startY - 10, dividerX + 1, sepY, 0x20FFFFFF);

        int statY = centerY + 28;
        int statLeft = leftX - 4;
        int statRight = leftX + fieldWidth + 4;
        context.fill(statLeft, statY - 2, statRight, statY + 22, 0x15FFFFFF);

        // 修改：使用 CONFIG.vortexManipulatorMaxFuel 替代 VortexManipulatorItem.MAX_FUEL
        Text fuelText = Text.translatable("gui.doctor_m.vm.fuel_label", fuel, CONFIG.vortexManipulatorMaxFuel)
                .formatted(fuel < 100 ? Formatting.RED : Formatting.GREEN);
        Text ohText = Text.translatable("gui.doctor_m.vm.heat_label", overheat)
                .formatted(overheat > 80 ? Formatting.RED : Formatting.YELLOW);

        context.drawTextWithShadow(this.textRenderer, fuelText, leftX, statY, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, ohText, leftX, statY + 12, 0xFFFFFF);

        int bottomLineY = centerY + 56;
        context.fill(bottomLeftX, bottomLineY, bottomRightX + bottomBtnW, bottomLineY + 1, 0x30FFFFFF);

        if (broken) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.doctor_m.vm.broken_label").formatted(Formatting.DARK_RED, Formatting.BOLD),
                    centerX, bottomLineY + 4, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
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