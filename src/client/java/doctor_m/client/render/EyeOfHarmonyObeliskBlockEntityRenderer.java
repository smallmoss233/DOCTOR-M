package doctor_m.client.render;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

import org.joml.Matrix4f;

import dev.amble.ait.AITMod;
import dev.amble.ait.client.models.decoration.TardisStarModel;
import dev.amble.ait.client.renderers.AITRenderLayers;

public class EyeOfHarmonyObeliskBlockEntityRenderer implements BlockEntityRenderer<EyeOfHarmonyObeliskBlockEntity> {

    public static final Identifier TARDIS_STAR_TEXTURE = new Identifier(AITMod.MOD_ID,
            "textures/environment/eye_of_harmony.png");

    private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0) / 2.0);
    private static ModelPart starModelCache = null;

    public EyeOfHarmonyObeliskBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public int getRenderDistance() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void render(EyeOfHarmonyObeliskBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.getWorld() == null) return;
        if (!entity.isActive()) return;
        if (!entity.isEyeVisible()) return;

        float delta = tickDelta + entity.getWorld().getTime();
        float scale = entity.getScale();

        matrices.push();
        matrices.translate(0, 55.0 + entity.getYOffset(), 0);

        renderStar(entity, delta, scale, matrices, vertexConsumers, overlay);
        renderShine(entity, delta, scale, matrices, vertexConsumers);

        matrices.pop();
    }

    private void renderStar(EyeOfHarmonyObeliskBlockEntity entity, float delta, float scale, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers, int overlay) {
        matrices.push();

        // ← 应用用户自定义缩放，默认 40f * 1.0 = 40f
        matrices.scale(40f * scale, 40f * scale, 40f * scale);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(delta));

        if (starModelCache == null) {
            starModelCache = TardisStarModel.getTexturedModelData().createModel();
        }

        starModelCache.render(matrices,
                vertexConsumers.getBuffer(AITRenderLayers.tardisEmissiveCullZOffset(TARDIS_STAR_TEXTURE, true)),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay,
                1.0f, 1.0f, 1.0f, 0.5f);

        matrices.scale(0.9f, 0.9f, 0.9f);
        starModelCache.render(matrices,
                vertexConsumers.getBuffer(AITRenderLayers.tardisEmissiveCullZOffset(TARDIS_STAR_TEXTURE, true)),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay,
                1.0f, 1.0f, 1.0f, 1.0f);

        matrices.pop();
    }

    private void renderShine(EyeOfHarmonyObeliskBlockEntity entity, float delta, float scale, MatrixStack matrices,
                             VertexConsumerProvider vertexConsumers) {
        matrices.push();

        // ← 光芒也同步缩放，默认 8f * 1.0 = 8f
        matrices.scale(8f * scale, 8f * scale, 8f * scale);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-delta));

        float time = entity.getWorld().getTime() + delta;
        float l = (time / 50120f);

        Random random = Random.create(entity.getPos().hashCode());
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(AITRenderLayers.getLightning());

        float m = Math.min(l > 0.8f ? (l - 0.8f) / 0.2f : 0.0f, 1.0f);

        for (int n = 0; n < 30; n++) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((random.nextFloat() * 360.0f)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((random.nextFloat() * 360.0f)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((random.nextFloat() * 360.0f)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((random.nextFloat() * 360.0f)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((random.nextFloat() * 360.0f)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((random.nextFloat() * 360.0f + l * 90.0f)));

            float o = random.nextFloat() * 10.0f + 10.0f + m * 10.0f;
            float p = random.nextFloat() * 0.5f + 1.0f + m * 2.0f;

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            int q = (int) (255f * (1.0f - m));

            putLightSourceVertex(vertexConsumer, matrix4f, q);
            putLightNegativeXTerminalVertex(vertexConsumer, matrix4f, o, p);
            putLightPositiveXTerminalVertex(vertexConsumer, matrix4f, o, p);

            putLightSourceVertex(vertexConsumer, matrix4f, q);
            putLightPositiveXTerminalVertex(vertexConsumer, matrix4f, o, p);
            putLightPositiveZTerminalVertex(vertexConsumer, matrix4f, o, p);

            putLightSourceVertex(vertexConsumer, matrix4f, q);
            putLightPositiveZTerminalVertex(vertexConsumer, matrix4f, o, p);
            putLightNegativeXTerminalVertex(vertexConsumer, matrix4f, o, p);

            putLightPositiveXTerminalVertex(vertexConsumer, matrix4f, o, p);
            putLightPositiveZTerminalVertex(vertexConsumer, matrix4f, o, p);
            putLightNegativeXTerminalVertex(vertexConsumer, matrix4f, o, p);
        }

        matrices.pop();
    }

    private static void putLightSourceVertex(VertexConsumer buffer, Matrix4f matrix, int alpha) {
        buffer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(255, 255, 255, alpha).next();
    }

    private static void putLightNegativeXTerminalVertex(VertexConsumer buffer, Matrix4f matrix, float radius, float width) {
        buffer.vertex(matrix, -HALF_SQRT_3 * width, radius, -0.5f * width)
                .color(255, 154, 0, 0).next();
    }

    private static void putLightPositiveXTerminalVertex(VertexConsumer buffer, Matrix4f matrix, float radius, float width) {
        buffer.vertex(matrix, HALF_SQRT_3 * width, radius, -0.5f * width)
                .color(255, 154, 0, 0).next();
    }

    private static void putLightPositiveZTerminalVertex(VertexConsumer buffer, Matrix4f matrix, float radius, float width) {
        buffer.vertex(matrix, 0.0f, radius, width)
                .color(255, 154, 0, 0).next();
    }
}