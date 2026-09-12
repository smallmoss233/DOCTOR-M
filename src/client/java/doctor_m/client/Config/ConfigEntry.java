package doctor_m.client.Config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public abstract class ConfigEntry extends ElementListWidget.Entry<ConfigEntry> {

    protected final String fieldName;
    protected final String nameKey;
    protected final String nameFallback;
    protected final List<ClickableWidget> widgets = new ArrayList<>();

    protected ConfigEntry(String fieldName, String nameFallback) {
        this.fieldName = fieldName;
        this.nameKey = "gui.doctor_m.config." + fieldName;
        this.nameFallback = nameFallback;
    }

    @Override
    public List<? extends Element> children() { return widgets; }

    @Override
    public List<? extends Selectable> selectableChildren() { return widgets; }

    protected boolean isTranslated() {
        String translated = I18n.translate(nameKey);
        return !translated.equals(nameKey);
    }

    protected Text label() {
        if (isTranslated()) {
            return Text.literal(I18n.translate(nameKey));
        }
        return Text.literal(nameFallback + " ⚠").formatted(Formatting.YELLOW);
    }

    public abstract void resetToDefault();

    protected void drawRowBackground(DrawContext ctx, int x, int y,
                                     int width, int height,
                                     int index, boolean hovered) {
        if (hovered) {
            ctx.fill(x - 6, y - 2, x + width + 6, y + height + 2, 0x30FFFFFF);
        } else if (index % 2 == 0) {
            ctx.fill(x - 6, y - 2, x + width + 6, y + height + 2, 0x18FFFFFF);
        }
    }

    protected void drawLabel(DrawContext ctx, int x, int y) {
        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                label(), x, y + 6, 0xFFFFFF);
    }

    protected static ButtonWidget createResetButton(Runnable onClick) {
        return ButtonWidget.builder(Text.literal("↻"), b -> onClick.run())
                .dimensions(0, 0, 20, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.doctor_m.config.reset.tooltip")))
                .build();
    }

    // ================================================================
    // 布尔
    // ================================================================
    public static class BoolEntry extends ConfigEntry {
        private final boolean defaultValue;
        private boolean value;
        private final ButtonWidget button;
        private final ButtonWidget resetButton;
        private final Consumer<Boolean> setter;

        public BoolEntry(String fieldName, String nameFallback,
                         boolean currentValue, boolean defaultValue,
                         Consumer<Boolean> setter) {
            super(fieldName, nameFallback);
            this.defaultValue = defaultValue;
            this.value = currentValue;
            this.setter = setter;

            this.button = ButtonWidget.builder(stateText(), b -> {
                this.value = !this.value;
                b.setMessage(stateText());
                this.setter.accept(this.value);
            }).dimensions(0, 0, 80, 20).build();

            this.resetButton = createResetButton(this::resetToDefault);

            widgets.add(button);
            widgets.add(resetButton);
        }

        private Text stateText() {
            return value
                    ? Text.translatable("gui.doctor_m.config.value.on").formatted(Formatting.GREEN)
                    : Text.translatable("gui.doctor_m.config.value.off").formatted(Formatting.RED);
        }

        @Override
        public void resetToDefault() {
            if (this.value != this.defaultValue) {
                this.value = this.defaultValue;
                this.button.setMessage(stateText());
                this.setter.accept(this.value);
            }
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x,
                           int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawRowBackground(ctx, x, y, entryWidth, entryHeight, index, hovered);
            drawLabel(ctx, x, y);

            // 行内布局：[label ...] [开关][重置]
            resetButton.setPosition(x + entryWidth - 24, y);
            button.setPosition(x + entryWidth - 108, y);

            button.render(ctx, mouseX, mouseY, tickDelta);
            resetButton.render(ctx, mouseX, mouseY, tickDelta);
        }
    }

    // ================================================================
    // 数值
    // ================================================================
    public static class NumberEntry extends ConfigEntry {
        private final ButtonWidget left, display, right, resetButton;
        private double value;
        private final double defaultValue;
        private final double step, min, max;
        private final boolean integer;
        private final DoubleConsumer setter;

        public NumberEntry(String fieldName, String nameFallback,
                           double currentValue, double defaultValue,
                           double min, double max, double step,
                           boolean integer, DoubleConsumer setter) {
            super(fieldName, nameFallback);
            this.defaultValue = defaultValue;
            this.value = currentValue;
            this.min = min;
            this.max = max;
            this.step = step;
            this.integer = integer;
            this.setter = setter;

            this.left = ButtonWidget.builder(Text.literal("◀"), b -> adjust(-step))
                    .dimensions(0, 0, 20, 20).build();
            this.display = ButtonWidget.builder(valueText(), b -> {})
                    .dimensions(0, 0, 80, 20).build();
            this.right = ButtonWidget.builder(Text.literal("▶"), b -> adjust(step))
                    .dimensions(0, 0, 20, 20).build();
            this.resetButton = createResetButton(this::resetToDefault);

            widgets.add(left);
            widgets.add(display);
            widgets.add(right);
            widgets.add(resetButton);
        }

        private Text valueText() {
            String s = integer
                    ? String.valueOf((long) value)
                    : String.format("%.2f", value);
            return Text.literal(s);
        }

        private void adjust(double rawDelta) {
            MinecraftClient mc = MinecraftClient.getInstance();
            long handle = mc.getWindow().getHandle();
            boolean shift = InputUtil.isKeyPressed(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean ctrl = InputUtil.isKeyPressed(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputUtil.isKeyPressed(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
            double multiplier = ctrl ? 100.0 : shift ? 10.0 : 1.0;
            double delta = rawDelta * multiplier;

            double nv = value + delta;
            nv = Math.max(min, Math.min(max, nv));
            if (integer) nv = Math.round(nv);
            if (nv != value) {
                value = nv;
                display.setMessage(valueText());
                setter.accept(value);
            }
        }

        @Override
        public void resetToDefault() {
            if (this.value != this.defaultValue) {
                this.value = this.defaultValue;
                this.display.setMessage(valueText());
                this.setter.accept(this.value);
            }
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x,
                           int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawRowBackground(ctx, x, y, entryWidth, entryHeight, index, hovered);
            drawLabel(ctx, x, y);

            // 行内布局：[label ...] [◀][数值][▶][重置]
            int rightEdge = x + entryWidth;
            resetButton.setPosition(rightEdge - 24, y);
            right.setPosition(rightEdge - 48, y);
            display.setPosition(rightEdge - 132, y);
            left.setPosition(rightEdge - 156, y);

            left.render(ctx, mouseX, mouseY, tickDelta);
            display.render(ctx, mouseX, mouseY, tickDelta);
            right.render(ctx, mouseX, mouseY, tickDelta);
            resetButton.render(ctx, mouseX, mouseY, tickDelta);
        }
    }
}