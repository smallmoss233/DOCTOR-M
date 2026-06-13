package doctor_m.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ShiftTooltipHelper {

    public static void addShiftTooltip(List<Text> tooltip, Text shortText, Text... detailedLines) {
        boolean shiftPressed = false;
        if (MinecraftClient.getInstance().getWindow() != null) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            shiftPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        }
        if (shiftPressed) {
            for (Text line : detailedLines) {
                tooltip.add(line);
            }
        } else {
            if (shortText != null && !shortText.getString().isEmpty()) {
                tooltip.add(shortText);
            }
            tooltip.add(Text.translatable("tooltip.doctor_m.hold_shift").formatted(Formatting.GRAY, Formatting.ITALIC));
        }
    }
}