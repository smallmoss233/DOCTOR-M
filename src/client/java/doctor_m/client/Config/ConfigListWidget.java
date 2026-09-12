package doctor_m.client.Config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

public class ConfigListWidget extends ElementListWidget<ConfigEntry> {

    public ConfigListWidget(MinecraftClient mc, int width, int height,
                            int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
    }

    public void addConfigEntry(ConfigEntry entry) {
        this.addEntry(entry);
    }

    @Override
    public int getRowWidth() {
        return Math.min(360, this.width - 40);
    }

    /** ★ 强制滚动条贴最右侧。方法名是 getScrollbarPositionX（1.20.1 映射）。 */
    @Override
    protected int getScrollbarPositionX() {
        return this.width - 8;
    }
}