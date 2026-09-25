package doctor_m.client.Config;

import doctor_m.config.ConfigManager;
import doctor_m.module.STP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DOCTORMConfigScreen extends Screen {

    private final Screen parent;
    private final List<ConfigEntry> allEntries = new ArrayList<>();
    private ConfigListWidget list;
    private TextFieldWidget searchField;
    private ButtonWidget onlyModifiedBtn;
    private boolean onlyModified = false;

    public DOCTORMConfigScreen(Screen parent) {
        super(Text.translatable("gui.doctor_m.config.title"));
        this.parent = parent;
    }

    public void addEntry(ConfigEntry entry) {
        this.allEntries.add(entry);
    }

    @Override
    protected void init() {
        // ---- 顶部：搜索框 + 仅修改过滤 ----
        int pad = 12;
        int topY = 26;

        this.searchField = new TextFieldWidget(this.textRenderer,
                pad, topY, 200, 18, Text.literal("Search"));
        this.searchField.setPlaceholder(Text.translatable("gui.doctor_m.config.search_hint")
                .formatted(Formatting.DARK_GRAY));
        this.searchField.setChangedListener(s -> applyFilter());
        this.addDrawableChild(this.searchField);

        this.onlyModifiedBtn = ButtonWidget.builder(onlyModifiedText(), b -> {
                    this.onlyModified = !this.onlyModified;
                    b.setMessage(onlyModifiedText());
                    applyFilter();
                })
                .dimensions(pad + 208, topY, 130, 18)
                .tooltip(Tooltip.of(Text.translatable("gui.doctor_m.config.only_modified.tooltip")))
                .build();
        this.addDrawableChild(this.onlyModifiedBtn);

        // ---- 中部：列表 ----
        this.list = new ConfigListWidget(this.client, this.width, this.height,
                topY + 24, this.height - 40, 24);
        this.addSelectableChild(this.list);

        applyFilter();

        // ---- 底部按钮 ----
        int cx = this.width / 2;
        int btnW = 100;
        int gap = 10;
        int totalW = btnW * 3 + gap * 2;
        int startX = cx - totalW / 2;
        int btnY = this.height - 28;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                b -> {
                    ConfigManager.loadConfig();
                    this.client.setScreen(this.parent);
                }).dimensions(startX, btnY, btnW, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.doctor_m.config.reset_all"),
                        b -> doctor_m$resetAll())
                .dimensions(startX + btnW + gap, btnY, btnW, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.doctor_m.config.reset_all.tooltip")))
                .build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.config.save"),
                b -> {
                    ConfigManager.saveConfig();
                    STP.invalidateCache();
                    this.client.setScreen(this.parent);
                }).dimensions(startX + (btnW + gap) * 2, btnY, btnW, 20).build());

        // 保留搜索框焦点，避免 applyFilter 时被清掉
        if (this.searchField != null) this.setFocused(this.searchField);
    }

    private Text onlyModifiedText() {
        Text base = Text.translatable(onlyModified
                ? "gui.doctor_m.config.only_modified.on"
                : "gui.doctor_m.config.only_modified.off");
        return onlyModified ? base.copy().formatted(Formatting.YELLOW) : base;
    }

    /** 根据搜索词和"仅修改"状态，重建列表。 */
    private void applyFilter() {
        if (this.list == null) return;

        String query = this.searchField == null
                ? ""
                : this.searchField.getText().trim().toLowerCase();

        // 缓存焦点，clearEntries 可能重置
        boolean searchFocused = this.searchField != null && this.searchField.isFocused();

        this.list.clearAllEntries();

        String lastCategory = null;
        for (ConfigEntry e : allEntries) {
            if (e instanceof ConfigEntry.HeaderEntry) continue;

            if (onlyModified && !e.isModified()) continue;
            if (!query.isEmpty()) {
                String name = e.getDisplayName().toLowerCase();
                String field = e.fieldName.toLowerCase();
                if (!name.contains(query) && !field.contains(query)) continue;
            }

            if (!Objects.equals(lastCategory, e.getCategory())) {
                this.list.addConfigEntry(new ConfigEntry.HeaderEntry(e.getCategory()));
                lastCategory = e.getCategory();
            }
            this.list.addConfigEntry(e);
        }

        if (searchFocused && this.searchField != null) {
            this.setFocused(this.searchField);
            this.searchField.setFocused(true);
        }
    }

    /** 全局重置：需要按住 Shift 才能触发。 */
    private void doctor_m$resetAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        long handle = mc.getWindow().getHandle();
        boolean shift = InputUtil.isKeyPressed(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (!shift) {
            if (mc.player != null) {
                mc.player.sendMessage(
                        Text.translatable("gui.doctor_m.config.reset_all.need_sneak")
                                .formatted(Formatting.YELLOW), true);
            }
            return;
        }

        for (ConfigEntry e : allEntries) {
            e.resetToDefault();
        }
        applyFilter();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        this.renderBackground(ctx);
        this.list.render(ctx, mouseX, mouseY, tickDelta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 10, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, tickDelta);
    }

    @Override
    public void close() {
        ConfigManager.loadConfig();
        this.client.setScreen(this.parent);
    }
}