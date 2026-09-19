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
        if (modelId == null) return;

        Identifier texId = entity.getMossTexture();
        if (texId == null) return;

        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();

        if (DIAGNOSED.add(modelId)) {
            MossTextureInfo.diagnose(rm, modelId, texId);
        }

        CachedModel cached = MODEL_CACHE.computeIfAbsent(modelId, id -> {
            MossBedrockModel model = MossBedrock.load(rm, id, null, texId);
            return new CachedModel(model.createModel(), model);
        });

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));   // ★ 加回来

        float yaw = entity.getMossYaw(tickDelta);
        if (yaw != 0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        }

        // 主纹理
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texId));
        cached.root().render(matrices, vc, light, overlay, 1f, 1f, 1f, 1f);

        // per-face 部分
        if (!cached.model().deferred().isEmpty()) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));  // ★ AmbleKit 的额外翻转
            MossPerFaceRenderer.render(
                    cached.root(),
                    cached.model().deferred(),
                    matrices,
                    vc,
                    light, overlay,
                    1f, 1f, 1f, 1f,
                    cached.model().textureWidth(),
                    cached.model().textureHeight());
            matrices.pop();
        }

        // 发光层
        Identifier emission = entity.getMossEmission();
        if (emission != null) {
            VertexConsumer vcE = vertexConsumers.getBuffer(
                    RenderLayer.getEntityCutoutNoCullZOffset(emission));
            cached.root().render(matrices, vcE,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, 1f, 1f, 1f, 1f);

            if (!cached.model().deferred().isEmpty()) {
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                MossPerFaceRenderer.render(
                        cached.root(),
                        cached.model().deferred(),
                        matrices,
                        vcE,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay,
                        1f, 1f, 1f, 1f,
                        cached.model().textureWidth(),
                        cached.model().textureHeight());
                matrices.pop();
            }
        }

        matrices.pop();
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