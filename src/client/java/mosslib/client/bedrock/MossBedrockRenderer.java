package mosslib.client.bedrock;

import mosslib.api.MossBedrockRenderable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class MossBedrockRenderer<T extends BlockEntity & MossBedrockRenderable>
        implements BlockEntityRenderer<T> {

    /** 方块坐标系下模型整体绕 X 轴翻转，使 Bedrock 模型的 Y-down 与方块世界对齐。 */
    private static final float MODEL_X_ROTATION_DEG = 180F;

    /** per-face 渲染相对根模型额外绕 Y 翻转（AmbleKit 约定）。 */
    private static final float PER_FACE_Y_ROTATION_DEG = 180F;

    /** 模型置于方块中心：方块占地 [0,1]，模型原点在方块西北角。 */
    private static final float MODEL_OFFSET_X = 0.5F;
    private static final float MODEL_OFFSET_Y = 0.0F;
    private static final float MODEL_OFFSET_Z = 0.5F;

    private static final float WHITE = 1F;

    private record CachedModel(ModelPart root, MossBedrockModel model) {}

    private static final Map<Identifier, CachedModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Set<Identifier> DIAGNOSED = ConcurrentHashMap.newKeySet();

    public MossBedrockRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public boolean rendersOutsideBoundingBox(T blockEntity) {
        return true;
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        Identifier modelId = entity.getMossModel();
        if (modelId == null) {
            return;
        }

        Identifier texId = entity.getMossTexture();
        if (texId == null) {
            return;
        }

        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();

        if (DIAGNOSED.add(modelId)) {
            MossTextureInfo.diagnose(rm, modelId, texId);
        }

        CachedModel cached = MODEL_CACHE.computeIfAbsent(modelId, id -> {
            MossBedrockModel model = MossBedrock.load(rm, id, null, texId);
            return new CachedModel(model.createModel(), model);
        });

        matrices.push();
        try {
            applyModelTransform(matrices, entity, tickDelta);

            // 主纹理层：实体半透明
            VertexConsumer mainVc = vertexConsumers.getBuffer(
                    RenderLayer.getEntityTranslucent(texId));
            renderModel(cached, matrices, mainVc, light, overlay);

            // 发光层：cutout no-cull z-offset
            Identifier emission = entity.getMossEmission();
            if (emission != null) {
                VertexConsumer emissionVc = vertexConsumers.getBuffer(
                        RenderLayer.getEntityCutoutNoCullZOffset(emission));
                renderModel(cached, matrices, emissionVc,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay);
            }
        } finally {
            matrices.pop();
        }
    }

    // 变换
    private void applyModelTransform(MatrixStack matrices, T entity, float tickDelta) {
        matrices.translate(MODEL_OFFSET_X, MODEL_OFFSET_Y, MODEL_OFFSET_Z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MODEL_X_ROTATION_DEG));

        float yaw = entity.getMossYaw(tickDelta);
        if (yaw != 0F) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        }
    }

    // 模型渲染
    private static void renderModel(CachedModel cached, MatrixStack matrices,
                                    VertexConsumer vc, int light, int overlay) {
        cached.root().render(matrices, vc, light, overlay,
                WHITE, WHITE, WHITE, WHITE);
        renderDeferred(cached, matrices, vc, light, overlay);
    }

    private static void renderDeferred(CachedModel cached, MatrixStack matrices,
                                       VertexConsumer vc, int light, int overlay) {
        MossBedrockModel model = cached.model();
        if (model.deferred().isEmpty()) {
            return;
        }

        matrices.push();
        try {
            matrices.multiply(
                    RotationAxis.POSITIVE_Y.rotationDegrees(PER_FACE_Y_ROTATION_DEG));

            MossPerFaceRenderer.render(
                    cached.root(),
                    model.deferred(),
                    matrices,
                    vc,
                    light, overlay,
                    WHITE, WHITE, WHITE, WHITE,
                    model.textureWidth(),
                    model.textureHeight());
        } finally {
            matrices.pop();
        }
    }

    public static void clearModelCache() {
        MODEL_CACHE.clear();
        DIAGNOSED.clear();
    }

    public static void invalidate(Identifier id) {
        MODEL_CACHE.remove(id);
        DIAGNOSED.remove(id);
    }
}