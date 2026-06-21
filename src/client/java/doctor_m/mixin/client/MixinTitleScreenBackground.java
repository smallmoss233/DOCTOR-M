package doctor_m.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import doctor_m.client.render.VortexBackgroundRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TitleScreen.class, priority = 1000)
public class MixinTitleScreenBackground {

    @Unique
    private VortexBackgroundRenderer doctor_m$vortexRenderer;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        Identifier texture = new Identifier("ait", "textures/vortex/darkness.png");
        doctor_m$vortexRenderer = new VortexBackgroundRenderer(texture);
        doctor_m$vortexRenderer.setSpeed(2.0f);
    }

    /**
     * 替换全景图渲染为涡旋背景
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/RotatingCubeMapRenderer;render(FF)V"
            )
    )
    private void renderVortexBackground(RotatingCubeMapRenderer instance, float delta, float alpha, Operation<Void> original) {
        if (doctor_m$vortexRenderer != null) {
            var client = MinecraftClient.getInstance();
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();
            var matrices = new net.minecraft.client.util.math.MatrixStack();
            doctor_m$vortexRenderer.render(matrices, width, height);
        } else {
            original.call(instance, delta, alpha);
        }
    }
}