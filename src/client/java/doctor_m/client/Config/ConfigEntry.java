package doctor_m.client.Config;

import doctor_m.config.ConfigGroups;
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

    // ---- 颜色常量 ----
    protected static final int COLOR_MODIFIED_BAR = 0xFFFFAA33;   // 修改指示条：橙黄
    protected static final int COLOR_ROW_ODD      = 0x1CFFFFFF;   // 偶数行背景
    protected static final int COLOR_ROW_EVEN     = 0x10FFFFFF;   // 奇数行背景
    protected static final int COLOR_ROW_HOVER    = 0x38FFFFFF;   // 悬停背景
    protected static final int COLOR_HEADER_BG    = 0x30FFAA33;   // 分组标题背景
    protected static final int COLOR_HEADER_BAR   = 0xFFFFAA33;   // 分组标题左侧条
    protected static final int COLOR_HEADER_TEXT  = 0xFFFFE0A0;   // 分组标题文字

    protected final String fieldName;
    protected final String nameKey;
    protected final String nameFallback;
    protected final String category;
    protected final List<ClickableWidget> widgets = new ArrayList<>();

    protected ConfigEntry(String fieldName, String nameFallback, String category) {
        this.fieldName = fieldName;
        this.nameKey = "gui.doctor_m.config." + fieldName;
        this.nameFallback = nameFallback;
        this.category = category;
    }

    public String getCategory() { return category; }

    public String getDisplayName() {
        if (isTranslated()) return I18n.translate(nameKey);
        return nameFallback;
    }

    public abstract boolean isModified();

    public abstract void resetToDefault();

    public boolean handleScroll(double mouseX, double mouseY, double amount) {
        return false;
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

    protected void drawRowBackground(DrawContext ctx, int x, int y, int width, int height,
                                     int index, boolean hovered, boolean modified) {
        int padX = 8;
        int padY = 1;
        int x0 = x - padX;
        int x1 = x + width + padX;
        int y0 = y - padY;
        int y1 = y + height + padY;

        int bg = hovered
                ? COLOR_ROW_HOVER
                : (index % 2 == 0 ? COLOR_ROW_ODD : COLOR_ROW_EVEN);
        ctx.fill(x0, y0, x1, y1, bg);

        // 顶部极淡高光，卡片边缘感
        ctx.fill(x0, y0, x1, y0 + 1, 0x10FFFFFF);

        // 修改过的项左侧橙条
        if (modified) {
            ctx.fill(x0, y0, x0 + 3, y1, COLOR_MODIFIED_BAR);
        }
    }

    protected void drawLabel(DrawContext ctx, int x, int y) {
        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                label(), x, y + 6, 0xFFFFFF);
    }

    /** 重置按钮：使用更清晰的字符，并给 tooltip。 */
    protected static ButtonWidget createResetButton(Runnable onClick) {
        return ButtonWidget.builder(Text.literal("↺"), b -> onClick.run())
                .dimensions(0, 0, 20, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.doctor_m.config.reset.tooltip")))
                .build();
    }

    // ================================================================
    // 分组标题（不可交互）
    // ================================================================
    public static class HeaderEntry extends ConfigEntry {
        private static final String SEP = "____header____";

        public HeaderEntry(String categoryId) {
            super(SEP + categoryId, categoryId, categoryId);
        }

        @Override public boolean isModified() { return false; }
        @Override public void resetToDefault() {}

        private Text displayText() {
            String key = ConfigGroups.LANG_PREFIX + category;
            String translated = I18n.translate(key);
            if (translated.equals(key)) {
                return Text.literal(category).formatted(Formatting.YELLOW);
            }
            return Text.literal(translated);
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x,
                           int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int padX = 8;
            int x0 = x - padX;
            int x1 = x + entryWidth + padX;
            int y0 = y - 1;
            int y1 = y + entryHeight + 1;

            // 分组标题背景色块
            ctx.fill(x0, y0, x1, y1, COLOR_HEADER_BG);

            // 左侧加粗橙条（视觉上明显区分于字段行）
            ctx.fill(x0, y0, x0 + 4, y1, COLOR_HEADER_BAR);

            // 标题文字（加粗橙黄色）
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    displayText().copy().formatted(Formatting.BOLD),
                    x + 2, y + (entryHeight - 8) / 2, COLOR_HEADER_TEXT);
        }
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

        public BoolEntry(String fieldName, String nameFallback, String category,
                         boolean currentValue, boolean defaultValue,
                         Consumer<Boolean> setter) {
            super(fieldName, nameFallback, category);
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

        @Override public boolean isModified() { return value != defaultValue; }

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
            drawRowBackground(ctx, x, y, entryWidth, entryHeight, index, hovered, isModified());
            drawLabel(ctx, x, y);

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

        public NumberEntry(String fieldName, String nameFallback, String category,
                           double currentValue, double defaultValue,
                           double min, double max, double step,
                           boolean integer, DoubleConsumer setter) {
            super(fieldName, nameFallback, category);
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
                    .dimensions(0, 0, 80, 20)
                    .tooltip(Tooltip.of(Text.literal(
                            fieldName + "\n"
                                    + "min=" + fmt(min) + "  max=" + fmt(max) + "  step=" + fmt(step)
                                    + "\n" + I18n.translate("gui.doctor_m.config.scroll_hint"))))
                    .build();
            this.right = ButtonWidget.builder(Text.literal("▶"), b -> adjust(step))
                    .dimensions(0, 0, 20, 20).build();
            this.resetButton = createResetButton(this::resetToDefault);

            widgets.add(left);
            widgets.add(display);
            widgets.add(right);
            widgets.add(resetButton);
        }

        private String fmt(double v) {
            if (integer) return String.valueOf((long) v);
            if (v == Math.floor(v) && Math.abs(v) < 1e9) return String.valueOf((long) v);
            return String.format("%.2f", v);
        }

        private Text valueText() {
            return Text.literal(fmt(value));
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
        public boolean isModified() {
            return integer
                    ? (long) value != (long) defaultValue
                    : Math.abs(value - defaultValue) > 1e-9;
        }

        @Override
        public boolean handleScroll(double mouseX, double mouseY, double amount) {
            if (!display.isMouseOver(mouseX, mouseY)) return false;
            adjust(amount > 0 ? step : -step);
            return true;
        }

        @Override
        public void resetToDefault() {
            if (isModified()) {
                this.value = this.defaultValue;
                this.display.setMessage(valueText());
                this.setter.accept(this.value);
            }
        }

        @Override
        public void render(DrawContext ctx, int index, int y, int x,
                           int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            drawRowBackground(ctx, x, y, entryWidth, entryHeight, index, hovered, isModified());
            drawLabel(ctx, x, y);

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