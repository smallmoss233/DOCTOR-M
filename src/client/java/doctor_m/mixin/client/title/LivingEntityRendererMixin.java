package doctor_m.mixin.client.title;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import doctor_m.client.util.id.GlowConditionChecker;
import doctor_m.client.util.id.GlowTextRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public class LivingEntityRendererMixin<T extends Entity> {

    @WrapOperation(
            method = "renderLabelIfPresent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
                    ordinal = 1
            )
    )
    private int doctor_m$glowName3D(
            TextRenderer instance,
            Text text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            VertexConsumerProvider vertexConsumers,
            TextRenderer.TextLayerType layerType,
            int backgroundColor,
            int light,
            Operation<Integer> original,
            @Local(argsOnly = true) T entity) {

        if (entity instanceof PlayerEntity player && GlowConditionChecker.shouldGlow(player)) {
            String name = text.getString();
            String title = GlowConditionChecker.getPlayerTitleDirect(player);
            return GlowTextRenderer.draw3DAlternating(
                    instance, name, title, x, y, shadow,
                    matrix, vertexConsumers, layerType,
                    backgroundColor, light
            );
        }

        return original.call(instance, text, x, y, color, shadow,
                matrix, vertexConsumers, layerType, backgroundColor, light);
    }
}