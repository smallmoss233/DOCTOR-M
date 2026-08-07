package doctor_m.mixin.client.doctor_m;

import dev.amble.ait.client.screens.SonicSettingsScreen;
import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import doctor_m.Item.data_itme.TracerItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SonicSettingsScreen.class)
public class SonicSettingsScreenMixin {

    @Shadow @Final private List<ButtonWidget> buttons;
    @Shadow private int left;
    @Shadow private int top;
    @Shadow private int bgWidth;
    @Shadow private int bgHeight;
    @Shadow @Final private BlockPos console;

    private boolean doctor_m$isTracerInPort() {
        if (MinecraftClient.getInstance().world == null) return false;
        if (!(MinecraftClient.getInstance().world.getBlockEntity(console) instanceof ConsoleBlockEntity consoleBe)) {
            return false;
        }
        ItemStack stored = consoleBe.getSonicScrewdriver();
        return !stored.isEmpty() && stored.getItem() instanceof TracerItem;
    }

    @Inject(method = "createButtons", at = @At("RETURN"))
    private void doctor_m$disableButtonsForTracer(CallbackInfo ci) {
        if (!doctor_m$isTracerInPort()) return;

        if (buttons.size() >= 4) {
            buttons.get(0).active = false;
            buttons.get(2).active = false;
            buttons.get(3).active = false;
        }
    }

    @Inject(method = "drawSonicScrewdriver", at = @At("HEAD"), cancellable = true)
    private void doctor_m$renderTracerInstead(DrawContext context, int x, int y, float scale, CallbackInfo ci) {
        if (!doctor_m$isTracerInPort()) return;

        ci.cancel();

        MinecraftClient client = MinecraftClient.getInstance();
        int centerX = left + bgWidth / 2;

        // === 严格贴合白色边框内侧的内容区域 ===
        // 背景纹理 216x130，白色边框分布：
        //   左：x=0~1 黑, x=2~3 白, x=4~211 内容, x=212~213 白, x=214~215 黑
        //   上：y=0~14 透明, y=15 黑, y=16~17 白, y=18~120 内容, y=121 白, y=122~128 按钮底, y=129 黑
        // 按钮在 y=109(top+109)，所以框底部止于 y=108，刚好不盖住按钮
        int leftMargin   = 4;   // 白色边框内侧
        int rightMargin  = 5;   // 白色边框内侧 (216 - 4 - 5 = 207, 对应纹理 x=4~210)
        int topMargin    = 18;  // 白色边框内侧
        int bottomMargin = 21;  // 框底部 = top + 18 + (130-18-21) = top + 109，刚好在按钮上方

        int boxX = left + leftMargin;
        int boxY = top + topMargin;
        int boxWidth = bgWidth - leftMargin - rightMargin;   // 207
        int boxHeight = bgHeight - topMargin - bottomMargin;   // 91
        int boxCenterY = boxY + boxHeight / 2;

        // 深色半透明背景
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xF0100010);
        // 内边框（蓝紫色），画在框内侧 1px 处，绝不覆盖白色边框
        int innerX = boxX + 1;
        int innerY = boxY + 1;
        int innerW = boxWidth - 2;
        int innerH = boxHeight - 2;
        context.fill(innerX, innerY, innerX + innerW, innerY + 1, 0xFF5000FF); // 上
        context.fill(innerX, innerY + innerH - 1, innerX + innerW, innerY + innerH, 0xFF5000FF); // 下
        context.fill(innerX, innerY, innerX + 1, innerY + innerH, 0xFF5000FF); // 左
        context.fill(innerX + innerW - 1, innerY, innerX + innerW, innerY + innerH, 0xFF5000FF); // 右

        // 三行文字在框内垂直居中
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.translatable("gui.doctor_m.tracer.sonic_screen_title"),
                centerX, boxCenterY - 20, 0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.translatable("gui.doctor_m.tracer.sonic_screen_status"),
                centerX, boxCenterY, 0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.translatable("gui.doctor_m.tracer.sonic_screen_hint"),
                centerX, boxCenterY + 20, 0xFFFFFF
        );
    }

    @Inject(method = "sendSonicChangePacket", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockSonicChangeForTracer(CallbackInfo ci) {
        if (doctor_m$isTracerInPort()) {
            ci.cancel();
        }
    }
}