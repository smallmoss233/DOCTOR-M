package mosslib.client.bedrock.entity;

import mosslib.api.BlinkingEntity;
import mosslib.api.MossAnimatedEntity;
import mosslib.client.bedrock.anim.MossAnimation;
import mosslib.client.bedrock.anim.MossAnimationPlayer;
import mosslib.client.bedrock.anim.MossAnimationRegistry;
import mosslib.client.bedrock.block.MossBedrock;
import mosslib.client.bedrock.block.MossBedrockModel;
import mosslib.client.bedrock.block.MossPerFaceRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class MossBedrockEntityRenderer<T extends LivingEntity & MossAnimatedEntity>
        extends LivingEntityRenderer<T, MossBedrockEntityRenderer.Stub<T>> {

    // ── 骨骼名 ──
    private static final String HEAD_BONE       = "Head";
    private static final String EYE_BALL_LEFT   = "EyeBallLeft";
    private static final String EYE_BALL_RIGHT  = "EyeBallRight";
    private static final String EYE_WHITE_LEFT  = "EyeWhiteLeft";
    private static final String EYE_WHITE_RIGHT = "EyeWhiteRight";

    // ── 头部限制 ──
    private static final float HEAD_YAW_LIMIT   = 60F;
    private static final float HEAD_PITCH_LIMIT = 50F;

    // ── 眼睛限制 ──
    private static final float EYE_BALL_RANGE  = 0.5F / 16F;
    private static final float EYE_YAW_LIMIT   = 20F;
    private static final float EYE_PITCH_LIMIT = 15F;

    private static final float HEAD_SHARE = 0.75F;

    // ── 模型 ──
    private static final float MODEL_SCALE    = 0.9375F;
    private static final float MODEL_Y_OFFSET = 0F;

    private static final Map<Identifier, MossBedrockModel> MODEL_CACHE =
            new ConcurrentHashMap<>();

    // =====================================================================
    // 空壳 EntityModel
    // =====================================================================

    public static class Stub<T extends LivingEntity> extends EntityModel<T> {
        public Stub() { super(RenderLayer::getEntityCutoutNoCull); }

        @Override public void setAngles(T e, float a, float b, float c, float d, float f) {}
        @Override public void render(MatrixStack m, VertexConsumer v, int l, int o,
                                     float r, float g, float bl, float a) {}
    }

    public MossBedrockEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new Stub<>(), 0.5F);
    }

    // =====================================================================
    // 渲染入口
    // =====================================================================

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        Identifier modelId = entity.getMossModel();
        Identifier texId   = entity.getMossTexture();
        if (modelId == null || texId == null) return;

        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        MossBedrockModel model = MODEL_CACHE.computeIfAbsent(modelId,
                id -> MossBedrock.load(rm, id, null, texId));

        ModelPart root = model.createModel();
        root.traverse().forEach(ModelPart::resetTransform);

        // 1) 应用 Bedrock 动画（主 + 叠加）
        applyAnimation(root, entity, tickDelta);

        // 2) 叠加 AI 头部朝向
        applyHeadLook(root, entity, tickDelta);

        // 3) 叠加眼睛瞄向 + 眨眼
        applyEyes(root, entity, tickDelta);

        // 4) 坐标系变换 + 渲染
        matrices.push();
        try {
            applyEntityTransform(matrices, entity, tickDelta);

            int overlay = LivingEntityRenderer.getOverlay(entity, 0.0F);

            // 主纹理层
            VertexConsumer vc = vertexConsumers.getBuffer(
                    RenderLayer.getEntityTranslucent(texId));
            root.render(matrices, vc, light, overlay, 1F, 1F, 1F, 1F);
            renderDeferred(root, model, matrices, vc, light, overlay);   // ★ 新增

            // 发光层
            Identifier emission = entity.getMossEmission();
            if (emission != null) {
                VertexConsumer vcE = vertexConsumers.getBuffer(
                        RenderLayer.getEntityCutoutNoCullZOffset(emission));
                root.render(matrices, vcE, 0xF000F0, overlay, 1F, 1F, 1F, 1F);
                renderDeferred(root, model, matrices, vcE, 0xF000F0, overlay);   // ★ 新增
            }
        } finally {
            matrices.pop();
        }

        if (this.hasLabel(entity)) {
            this.renderLabelIfPresent(entity, entity.getDisplayName(),
                    matrices, vertexConsumers, light);
        }
    }

    @Override
    public Identifier getTexture(T entity) {
        return entity.getMossTexture();
    }

    // =====================================================================
    // per-face 渲染
    // =====================================================================

    /**
     * 渲染 per-face UV 的 cube（`deferred` 列表）。
     *
     * <p>这些 cube 不进入 ModelPart，需要单独用 {@link MossPerFaceRenderer} 渲染。
     * 每个 cube 通过 bonePath 找到对应 ModelPart，用其当前动画/变换状态叠加。</p>
     */
    private static void renderDeferred(ModelPart root, MossBedrockModel model,
                                       MatrixStack matrices,
                                       VertexConsumer vc, int light, int overlay) {
        if (model.deferred().isEmpty()) return;

        matrices.push();
        try {

            MossPerFaceRenderer.render(
                    root,
                    model.deferred(),
                    matrices,
                    vc,
                    light, overlay,
                    1F, 1F, 1F, 1F,
                    model.textureWidth(),
                    model.textureHeight());
        } finally {
            matrices.pop();
        }
    }

    // =====================================================================
    // 动画 —— 主 + 叠加
    // =====================================================================

    private static <E extends LivingEntity & MossAnimatedEntity>
    void applyAnimation(ModelPart root, E entity, float tickDelta) {

        // ── 1. 主动画（姿势 / 走路 / 生气 / 交易 ...） ──
        Identifier mainId = entity.getMossAnimationId();
        if (mainId != null) {
            MossAnimation main = MossAnimationRegistry.getInstance().get(mainId);
            if (main != null) {
                double elapsed = entity.getAnimationElapsedMs() / 1000.0
                        + tickDelta / 20.0;
                MossAnimationPlayer.apply(root, main, elapsed);
            }
        }

        // ── 2. 叠加动画（待机偶发动作） ──
        Identifier overlayId = entity.getMossOverlayAnimationId();
        if (overlayId != null) {
            MossAnimation overlay = MossAnimationRegistry.getInstance().get(overlayId);
            if (overlay != null) {
                double elapsed = entity.getMossOverlayElapsedMs() / 1000.0
                        + tickDelta / 20.0;
                MossAnimationPlayer.apply(root, overlay, elapsed);
            }
        }
    }

    // =====================================================================
    // 头部 —— 安全转向
    // =====================================================================

    private static <E extends LivingEntity & MossAnimatedEntity>
    void applyHeadLook(ModelPart root, E entity, float tickDelta) {

        ModelPart head = findBone(root, HEAD_BONE);
        if (head == null) return;

        float headYaw = MathHelper.lerpAngleDegrees(tickDelta,
                entity.prevHeadYaw, entity.headYaw);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta,
                entity.prevBodyYaw, entity.bodyYaw);
        float pitch   = MathHelper.lerp(tickDelta,
                entity.prevPitch, entity.getPitch());

        float relYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);
        relYaw = MathHelper.clamp(relYaw, -HEAD_YAW_LIMIT, HEAD_YAW_LIMIT);
        pitch  = MathHelper.clamp(pitch,  -HEAD_PITCH_LIMIT, HEAD_PITCH_LIMIT);

        head.yaw   += (float) Math.toRadians(relYaw);
        head.pitch += (float) Math.toRadians(pitch);
    }

    // =====================================================================
    // 眼睛 —— 偏移 + 眨眼
    // =====================================================================

    private static <E extends LivingEntity & MossAnimatedEntity>
    void applyEyes(ModelPart root, E entity, float tickDelta) {

        ModelPart ballL  = findBone(root, EYE_BALL_LEFT);
        ModelPart ballR  = findBone(root, EYE_BALL_RIGHT);
        ModelPart whiteL = findBone(root, EYE_WHITE_LEFT);
        ModelPart whiteR = findBone(root, EYE_WHITE_RIGHT);

        if (ballL == null && ballR == null) return;

        float headYaw = MathHelper.lerpAngleDegrees(tickDelta,
                entity.prevHeadYaw, entity.headYaw);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta,
                entity.prevBodyYaw, entity.bodyYaw);
        float pitch   = MathHelper.lerp(tickDelta,
                entity.prevPitch, entity.getPitch());

        float relYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);

        float eyeYaw   = MathHelper.clamp(relYaw * (1F - HEAD_SHARE),
                -EYE_YAW_LIMIT, EYE_YAW_LIMIT);
        float eyePitch = MathHelper.clamp(pitch * (1F - HEAD_SHARE),
                -EYE_PITCH_LIMIT, EYE_PITCH_LIMIT);

        float offsetX = (float) Math.tan(Math.toRadians(eyeYaw))   * EYE_BALL_RANGE * 2F;
        float offsetY = (float) Math.tan(Math.toRadians(eyePitch)) * EYE_BALL_RANGE * 2F;

        offsetX = MathHelper.clamp(offsetX, -EYE_BALL_RANGE, EYE_BALL_RANGE);
        offsetY = MathHelper.clamp(offsetY, -EYE_BALL_RANGE, EYE_BALL_RANGE);

        if (ballL != null) {
            ballL.pivotX += offsetX;
            ballL.pivotY += -offsetY;
        }
        if (ballR != null) {
            ballR.pivotX += offsetX;
            ballR.pivotY += -offsetY;
        }

        // 眨眼
        if (entity instanceof BlinkingEntity blinking && blinking.isBlinking()) {
            float scaleY = blinking.getBlinkScale();
            if (whiteL != null) whiteL.yScale = scaleY;
            if (whiteR != null) whiteR.yScale = scaleY;
            if (ballL  != null) ballL.yScale  = scaleY;
            if (ballR  != null) ballR.yScale  = scaleY;
        }
    }

    // =====================================================================
    // 骨骼查找
    // =====================================================================

    private static ModelPart findBone(ModelPart root, String name) {
        return root.traverse()
                .filter(p -> p.hasChild(name))
                .findFirst()
                .map(p -> p.getChild(name))
                .orElse(null);
    }

    // =====================================================================
    // 实体坐标系变换
    // =====================================================================

    private static <E extends LivingEntity> void applyEntityTransform(
            MatrixStack matrices, E entity, float tickDelta) {

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta,
                entity.prevBodyYaw, entity.bodyYaw);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.translate(0, MODEL_Y_OFFSET, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));
        matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}