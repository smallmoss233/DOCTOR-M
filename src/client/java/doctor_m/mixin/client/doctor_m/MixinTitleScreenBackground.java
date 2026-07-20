package doctor_m.mixin.client.doctor_m;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import doctor_m.client.render.VortexBackgroundRenderer;
import doctor_m.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
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
        // 从配置读取开关，但无论开闭都创建（避免空指针）
        Identifier texture = new Identifier("ait", "textures/vortex/darkness.png");
        doctor_m$vortexRenderer = VortexBackgroundRenderer.getInstance(texture);
        doctor_m$vortexRenderer.setSpeed(2.0f);
    }

    /**
     * 替换全景图渲染为涡旋背景（带开关控制）
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/RotatingCubeMapRenderer;render(FF)V"
            )
    )
    private void renderVortexBackground(
            RotatingCubeMapRenderer instance,
            float delta,
            float alpha,
            Operation<Void> original
    ) {
        // 读取配置开关
        if (ConfigManager.getConfig().enableVortexTitleBackground) {
            if (doctor_m$vortexRenderer != null) {
                var client = MinecraftClient.getInstance();
                int width = client.getWindow().getScaledWidth();
                int height = client.getWindow().getScaledHeight();
                MatrixStack matrices = new MatrixStack();
                doctor_m$vortexRenderer.render(matrices, width, height);
            } else {
                // 如果渲染器未初始化，回退到原版
                original.call(instance, delta, alpha);
            }
        } else {
            // 开关关闭，使用原版全景图
            original.call(instance, delta, alpha);
        }
    }
}