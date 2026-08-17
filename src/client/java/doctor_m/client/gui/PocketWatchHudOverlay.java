package doctor_m.client.gui;

import doctor_m.Item.data_itme.TimeKyeFragment.PocketWatchItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PocketWatchHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        ItemStack pocketWatch = null;
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        if (mainHand.getItem() instanceof PocketWatchItem && PocketWatchItem.isOpen(mainHand)) {
            pocketWatch = mainHand;
        } else if (offHand.getItem() instanceof PocketWatchItem && PocketWatchItem.isOpen(offHand)) {
            pocketWatch = offHand;
        }

        if (pocketWatch == null) return;

        long timeOfDay = player.getWorld().getTimeOfDay() % 24000L;
        String formattedTime = formatMinecraftTime12Hour(timeOfDay);

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = screenWidth / 2;
        int y = screenHeight - 56;

        Text displayText = Text.literal("☀ " + formattedTime + " · ")
                .append(Text.translatable(getTimeOfDayKey(timeOfDay)))
                .formatted(Formatting.GOLD);

        int textWidth = client.textRenderer.getWidth(displayText);
        drawContext.drawTextWithShadow(client.textRenderer, displayText, x - textWidth / 2, y, 0xFFD700);
    }

    private String getTimeOfDayKey(long time) {
        if (time >= 0 && time < 6000) {
            return "tooltip.doctor_m.pocket_watch.time.morning";
        } else if (time >= 6000 && time < 11000) {
            return "tooltip.doctor_m.pocket_watch.time.afternoon";
        } else if (time >= 11000 && time < 13000) {
            return "tooltip.doctor_m.pocket_watch.time.evening";
        } else {
            return "tooltip.doctor_m.pocket_watch.time.night";
        }
    }

    private String formatMinecraftTime12Hour(long time) {
        int totalMinutes = (int) ((time * 60L / 1000) + 360) % 1440;
        int hours24 = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        int hours12 = hours24 % 12;
        if (hours12 == 0) hours12 = 12;
        String ampm = hours24 < 12 ? "AM" : "PM";

        return String.format("%d:%02d %s", hours12, minutes, ampm);
    }
}