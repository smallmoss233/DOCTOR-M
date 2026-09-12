package doctor_m.client.Config;

import doctor_m.config.ConfigManager;
import doctor_m.module.STP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class DOCTORMConfigScreen extends Screen {

    private final Screen parent;
    private final List<ConfigEntry> entries = new ArrayList<>();
    private ConfigListWidget list;

    public DOCTORMConfigScreen(Screen parent) {
        super(Text.translatable("gui.doctor_m.config.title"));
        this.parent = parent;
    }

    public void addEntry(ConfigEntry entry) {
        this.entries.add(entry);
    }

    @Override
    protected void init() {
        this.list = new ConfigListWidget(this.client, this.width, this.height,
                32, this.height - 36, 24);
        for (ConfigEntry e : entries) {
            this.list.addConfigEntry(e);
        }
        this.addSelectableChild(this.list);

        int cx = this.width / 2;
        int btnW = 100;
        int gap = 10;
        int totalW = btnW * 3 + gap * 2;
        int startX = cx - totalW / 2;
        int btnY = this.height - 28;

        // 左：取消
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.cancel"),
                b -> {
                    ConfigManager.loadConfig();
                    this.client.setScreen(this.parent);
                }).dimensions(startX, btnY, btnW, 20).build());

        // 中：重置（需潜行）
        ButtonWidget resetAllBtn = ButtonWidget.builder(
                        Text.translatable("gui.doctor_m.config.reset_all"),
                        b -> doctor_m$resetAll())
                .dimensions(startX + btnW + gap, btnY, btnW, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.doctor_m.config.reset_all.tooltip")))
                .build();
        this.addDrawableChild(resetAllBtn);

        // 右：保存
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.config.save"),
                b -> {
                    ConfigManager.saveConfig();
                    STP.invalidateCache();
                    this.client.setScreen(this.parent);
                }).dimensions(startX + (btnW + gap) * 2, btnY, btnW, 20).build());
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

        for (ConfigEntry e : entries) {
            e.resetToDefault();
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        this.renderBackground(ctx);
        this.list.render(ctx, mouseX, mouseY, tickDelta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 12, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, tickDelta);
    }

    @Override
    public void close() {
        ConfigManager.loadConfig();
        this.client.setScreen(this.parent);
    }
}