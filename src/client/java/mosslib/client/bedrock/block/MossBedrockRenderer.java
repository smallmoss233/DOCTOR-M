package mosslib.client.bedrock.block;

import mosslib.api.MossBedrockRenderable;
import mosslib.api.MossAnimatedBlockEntity;
import mosslib.client.bedrock.anim.MossAnimation;
import mosslib.client.bedrock.anim.MossAnimationPlayer;
import mosslib.client.bedrock.anim.MossAnimationRegistry;
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class MossBedrockRenderer<T extends BlockEntity & MossBedrockRenderable>
        implements BlockEntityRenderer<T> {

    // ── 常量 ─────────────────────────────────────────
    private static final float MODEL_X_ROTATION_DEG = 180F;
    private static final float PER_FACE_Y_ROTATION_DEG = 180F;
    private static final float MODEL_OFFSET_X = 0.5F;
    private static final float MODEL_OFFSET_Y = 0.0F;
    private static final float MODEL_OFFSET_Z = 0.5F;
    private static final float WHITE = 1F;

    // ── 缓存 ─────────────────────────────────────────
    /** 模型模板，按 modelId 共享。 */
    private static final Map<Identifier, MossBedrockModel> MODEL_CACHE =
            new ConcurrentHashMap<>();

    /** 只诊断一次的模型集合。 */
    private static final Set<Identifier> DIAGNOSED = ConcurrentHashMap.newKeySet();

    /** 静态模型的共享 ModelPart（惰性 createModel，按 modelId 缓存）。 */
    private static final Map<Identifier, ModelPart> STATIC_ROOT =
            new ConcurrentHashMap<>();

    /** 动画 BE 的工作副本 ModelPart（每 BE 一份，BE 被回收即释放）。 */
    private static final Map<BlockEntity, ModelPart> WORKING_ROOT =
            Collections.synchronizedMap(new WeakHashMap<>());

    public MossBedrockRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public boolean rendersOutsideBoundingBox(T blockEntity) {
        return true;
    }

// =====================================================================
// 渲染入口
// =====================================================================

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

        MossBedrockModel model = MODEL_CACHE.computeIfAbsent(modelId,
                id -> MossBedrock.load(rm, id, null, texId));

        ModelPart root = resolveRoot(entity, modelId, model, tickDelta);

        matrices.push();
        try {
            applyModelTransform(matrices, entity, tickDelta);

            // 主纹理层
            VertexConsumer mainVc = vertexConsumers.getBuffer(
                    RenderLayer.getEntityTranslucent(texId));
            renderModel(root, model, matrices, mainVc, light, overlay);

            // 发光层
            Identifier emission = entity.getMossEmission();
            if (emission != null) {
                VertexConsumer emissionVc = vertexConsumers.getBuffer(
                        RenderLayer.getEntityCutoutNoCullZOffset(emission));
                renderModel(root, model, matrices, emissionVc,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay);
            }
        } finally {
            matrices.pop();
        }
    }

// =====================================================================
// 根节点决议 + 动画应用
// =====================================================================

    private ModelPart resolveRoot(T entity, Identifier modelId,
                                  MossBedrockModel model, float tickDelta) {

        // 情况 1：BE 没实现 MossAnimatedBlockEntity → 静态
        if (!(entity instanceof MossAnimatedBlockEntity animated)) {
            return STATIC_ROOT.computeIfAbsent(modelId, id -> model.createModel());
        }

        // 情况 2：BE 实现了接口，但当前没有动画 → 静态
        Identifier animId = animated.getMossAnimationId();
        if (animId == null) {
            return STATIC_ROOT.computeIfAbsent(modelId, id -> model.createModel());
        }

        // 情况 3：从注册表解析动画；查不到 → 静态
        MossAnimation anim = MossAnimationRegistry.getInstance().get(animId);
        if (anim == null) {
            return STATIC_ROOT.computeIfAbsent(modelId, id -> model.createModel());
        }

        // 情况 4：有动画 → 用工作副本
        ModelPart working = WORKING_ROOT.computeIfAbsent(entity, e -> model.createModel());

        // 每帧先回到初始姿态，避免上一帧残留
        working.traverse().forEach(ModelPart::resetTransform);

        double elapsedSeconds = animated.getAnimationElapsedMs() / 1000.0
                + tickDelta / 20.0;

        MossAnimationPlayer.apply(working, anim, elapsedSeconds);
        return working;
    }

    // =====================================================================
    // 变换
    // =====================================================================

    private void applyModelTransform(MatrixStack matrices, T entity, float tickDelta) {
        matrices.translate(MODEL_OFFSET_X, MODEL_OFFSET_Y, MODEL_OFFSET_Z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MODEL_X_ROTATION_DEG));

        float yaw = entity.getMossYaw(tickDelta);
        if (yaw != 0F) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        }
    }

    // =====================================================================
    // 模型渲染
    // =====================================================================

    private static void renderModel(ModelPart root, MossBedrockModel model,
                                    MatrixStack matrices,
                                    VertexConsumer vc, int light, int overlay) {
        root.render(matrices, vc, light, overlay, WHITE, WHITE, WHITE, WHITE);
        renderDeferred(root, model, matrices, vc, light, overlay);
    }

    private static void renderDeferred(ModelPart root, MossBedrockModel model,
                                       MatrixStack matrices,
                                       VertexConsumer vc, int light, int overlay) {
        if (model.deferred().isEmpty()) return;

        matrices.push();
        try {
            matrices.multiply(
                    RotationAxis.POSITIVE_Y.rotationDegrees(PER_FACE_Y_ROTATION_DEG));

            MossPerFaceRenderer.render(
                    root,
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

    // =====================================================================
    // 缓存管理
    // =====================================================================

    public static void clearModelCache() {
        MODEL_CACHE.clear();
        DIAGNOSED.clear();
        STATIC_ROOT.clear();
        WORKING_ROOT.clear();
    }

    public static void invalidate(Identifier id) {
        MODEL_CACHE.remove(id);
        DIAGNOSED.remove(id);
        STATIC_ROOT.remove(id);
    }
}