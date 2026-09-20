package mosslib.client.bedrock.entity;

import mosslib.api.BlinkingEntity;
import mosslib.api.MossAnimatedEntity;
import mosslib.client.bedrock.anim.MossAnimation;
import mosslib.client.bedrock.anim.MossAnimationPlayer;
import mosslib.client.bedrock.anim.MossAnimationRegistry;
import mosslib.client.bedrock.block.MossBedrock;
import mosslib.client.bedrock.block.MossBedrockModel;
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

    // ── 骨骼名（按你的 geo.json 改） ──
    private static final String HEAD_BONE       = "Head";
    private static final String EYE_BALL_LEFT   = "EyeBallLeft";
    private static final String EYE_BALL_RIGHT  = "EyeBallRight";
    private static final String EYE_WHITE_LEFT  = "EyeWhiteLeft";
    private static final String EYE_WHITE_RIGHT = "EyeWhiteRight";

    // ── 头部限制 ──
    /**
     * 头部相对身体的最大偏角（度）。
     * <p>硬上限：头永远不可能偏超过这个值。玩家绕到背后时，头最多偏
     * {@code HEAD_YAW_LIMIT}° 就停了，不会出现"扭头 180° 看背后"。</p>
     */
    private static final float HEAD_YAW_LIMIT   = 60F;
    private static final float HEAD_PITCH_LIMIT = 50F;

    // ── 眼睛限制 ──
    /**
     * 眼珠相对眼白的最大偏移（格）。
     * <p>= (眼白边长 - 眼珠边长) / 2 / 16。你的眼睛是 2×2 眼白 + 1×1 眼珠，
     * 所以 = (2 - 1) / 2 / 16 = 1/32 ≈ 0.03125。</p>
     */
    private static final float EYE_BALL_RANGE  = 0.5F / 16F;
    private static final float EYE_YAW_LIMIT   = 20F;
    private static final float EYE_PITCH_LIMIT = 15F;

    /** 头部承担的比例。剩余部分给眼睛。 */
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

        // 1) 应用 Bedrock 动画
        applyAnimation(root, entity, tickDelta);

        // 2) 叠加 AI 头部朝向（安全 clamp）
        applyHeadLook(root, entity, tickDelta);

        // 3) 叠加眼睛瞄向
        applyEyes(root, entity, tickDelta);

        // 4) 坐标系变换 + 渲染
        matrices.push();
        try {
            applyEntityTransform(matrices, entity, tickDelta);

            int overlay = LivingEntityRenderer.getOverlay(entity, 0.0F);

            VertexConsumer vc = vertexConsumers.getBuffer(
                    RenderLayer.getEntityTranslucent(texId));
            root.render(matrices, vc, light, overlay, 1F, 1F, 1F, 1F);

            Identifier emission = entity.getMossEmission();
            if (emission != null) {
                VertexConsumer vcE = vertexConsumers.getBuffer(
                        RenderLayer.getEntityCutoutNoCullZOffset(emission));
                root.render(matrices, vcE, 0xF000F0, overlay, 1F, 1F, 1F, 1F);
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
// 动画
// =====================================================================

    private static <E extends LivingEntity & MossAnimatedEntity>
    void applyAnimation(ModelPart root, E entity, float tickDelta) {

        Identifier animId = entity.getMossAnimationId();
        if (animId == null) return;

        MossAnimation anim = MossAnimationRegistry.getInstance().get(animId);
        if (anim == null) return;

        double elapsedSeconds = entity.getAnimationElapsedMs() / 1000.0
                + tickDelta / 20.0;

        MossAnimationPlayer.apply(root, anim, elapsedSeconds);
    }

// =====================================================================
// 头部 —— 安全转向
// =====================================================================

    /**
     * 把 AI 的 {@code headYaw / headPitch} 转成头部骨骼的相对旋转。
     *
     * <p>三道安全措施，保证不会出现"头转 360° 看背后"的诡异情况：</p>
     * <ol>
     *   <li>{@link MathHelper#wrapDegrees} 把角度差折叠到 [-180, 180]</li>
     *   <li>硬 clamp 到 ±{@link #HEAD_YAW_LIMIT}°</li>
     *   <li>只作用在 head 骨骼上，不会带动身体</li>
     * </ol>
     */
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

        // 差值包绕到 [-180, 180]
        float relYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);

        // 硬 clamp
        relYaw = MathHelper.clamp(relYaw, -HEAD_YAW_LIMIT, HEAD_YAW_LIMIT);
        pitch  = MathHelper.clamp(pitch,  -HEAD_PITCH_LIMIT, HEAD_PITCH_LIMIT);

        // ★ 改这里：去掉负号
        head.yaw   += (float) Math.toRadians(relYaw);
        head.pitch += (float) Math.toRadians(pitch);
    }

// =====================================================================
// 眼睛 —— 眼白内偏移
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

        // 眼睛只承担"头没转到的剩余部分"
        float eyeYaw   = MathHelper.clamp(relYaw * (1F - HEAD_SHARE),
                -EYE_YAW_LIMIT, EYE_YAW_LIMIT);
        float eyePitch = MathHelper.clamp(pitch * (1F - HEAD_SHARE),
                -EYE_PITCH_LIMIT, EYE_PITCH_LIMIT);

        // 角度 → 像素偏移
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

        // ★ 只改这一行：180F - bodyYaw  →  -bodyYaw
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

        matrices.translate(0, MODEL_Y_OFFSET, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));
        matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}