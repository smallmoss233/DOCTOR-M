package doctor_m.client.Shield;

import dev.amble.ait.client.models.machines.ShieldsModel;
import doctor_m.Item.data_itme.ForceFieldShieldItem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class ForceFieldClientRenderer {
    private static final Identifier SHIELDS_TEXTURE =
            new Identifier("doctor_m", "textures/environment/shields.png");

    private static final ShieldsModel SHIELDS_MODEL = new ShieldsModel(
            ShieldsModel.getTexturedModelData().createModel());

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ForceFieldClientRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (ForceFieldShieldItem.isForceFieldActive(player)) {
                renderForceField(context, player);
            }
        }
    }

    private static void renderForceField(WorldRenderContext context, PlayerEntity player) {
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        float tickDelta = context.tickDelta();
        if (consumers == null) return;

        matrices.push();

        // 插值到玩家位置（腰部中心）
        double x = player.lastRenderX + (player.getX() - player.lastRenderX) * tickDelta;
        double y = player.lastRenderY + (player.getY() - player.lastRenderY) * tickDelta;
        double z = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * tickDelta;
        Vec3d cameraPos = context.camera().getPos();

        matrices.translate(
                x - cameraPos.x,
                y - cameraPos.y + player.getHeight() / 2.0,
                z - cameraPos.z
        );

        // ========== 自定义动画 ==========
        float time = (player.age + tickDelta) * 0.03f;

        // 1. 绕 Y 轴自转（每秒约 40 度，可自行调快慢）
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 40f));

        // 2. 呼吸脉动（缩放 ±3%，让力场有"呼吸"感）
        float pulse = 1.0f + (float) Math.sin(time * 3f) * 0.03f;
        matrices.scale(pulse, pulse, pulse);

        // 3. 可选：轻微倾斜，让旋转不那么单调
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(time * 0.5f) * 2f));

        // ========== 普通半透明渲染 ==========
        // 不再使用 getEnergySwirl，直接读取你的 PNG 原色
        VertexConsumer vertexConsumer = consumers.getBuffer(
                RenderLayer.getEntityTranslucent(SHIELDS_TEXTURE)
        );

        // 颜色乘数传 (1,1,1) 白色，这样纹理里你画的红色会原样显示
        // alpha 0.5f 控制整体半透明程度
        SHIELDS_MODEL.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.DEFAULT_UV,
                1.0f, 1.0f, 1.0f, 0.5f);

        matrices.pop();
    }
}