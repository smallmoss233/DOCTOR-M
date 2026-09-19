package mosslib.client.bedrock;

import mosslib.api.MossBedrockRenderable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class MossBedrockRenderer<T extends BlockEntity & MossBedrockRenderable>
        implements BlockEntityRenderer<T> {

    /** 模型缓存：ID → ModelPart。所有方块共享，相同 ID 只 build 一次。 */
    private static final Map<Identifier, ModelPart> MODEL_CACHE = new ConcurrentHashMap<>();

    public MossBedrockRenderer(BlockEntityRendererFactory.Context ctx) {
        // 不需要 context，留着以后可能要
    }

    @Override
    public boolean rendersOutsideBoundingBox(T blockEntity) {
        return true;
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        Identifier modelId = entity.getMossModel();
        if (modelId == null) return;

        ModelPart root = MODEL_CACHE.computeIfAbsent(modelId, id -> {
            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
            return MossBedrock.load(rm, id).createModel();
        });

        matrices.push();

        // 方块中心偏移
        matrices.translate(0.5, 0.0, 0.5);

        // Bedrock Y 轴朝下 → Java Y 轴朝上，翻转过来
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180f));

        // 朝向
        float yaw = entity.getMossYaw(tickDelta);
        if (yaw != 0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        }

        // 主纹理
        Identifier tex = entity.getMossTexture();
        if (tex != null) {
            root.render(matrices,
                    vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(tex)),
                    light, overlay,
                    1f, 1f, 1f, 1f);
        }

        // 发光层（可选）
        Identifier emission = entity.getMossEmission();
        if (emission != null) {
            root.render(matrices,
                    vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCullZOffset(emission)),
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay,
                    1f, 1f, 1f, 1f);
        }

        matrices.pop();
    }

    /** 资源重载 / 换模型时调用 */
    public static void clearModelCache() {
        MODEL_CACHE.clear();
    }

    /** 只清某一个模型 */
    public static void invalidate(Identifier id) {
        MODEL_CACHE.remove(id);
    }
}