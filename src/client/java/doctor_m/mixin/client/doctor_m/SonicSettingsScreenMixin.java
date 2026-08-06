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

        // 0=apply, 1=back, 2=left arrow, 3=right arrow
        if (buttons.size() >= 4) {
            buttons.get(0).active = false; // apply
            buttons.get(2).active = false; // left arrow
            buttons.get(3).active = false; // right arrow
        }
    }

    @Inject(method = "drawSonicScrewdriver", at = @At("HEAD"), cancellable = true)
    private void doctor_m$renderTracerInstead(DrawContext context, int x, int y, float scale, CallbackInfo ci) {
        if (!doctor_m$isTracerInPort()) return;

        ci.cancel();

        MinecraftClient client = MinecraftClient.getInstance();
        int centerX = left + bgWidth / 2;
        int centerY = top + bgHeight / 2 - 10;

        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal("§d§lKey to Time Tracer"),
                centerX, centerY - 15, 0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal("§7Temporal trace active"),
                centerX, centerY + 5, 0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal("§8Use Telepathic Circuits to locate fragments"),
                centerX, centerY + 25, 0xFFFFFF
        );
    }

    @Inject(method = "sendSonicChangePacket", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockSonicChangeForTracer(CallbackInfo ci) {
        if (doctor_m$isTracerInPort()) {
            ci.cancel();
        }
    }
}