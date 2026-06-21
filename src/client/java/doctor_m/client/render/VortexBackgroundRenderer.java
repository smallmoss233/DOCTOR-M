package doctor_m.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class VortexBackgroundRenderer {
    private final Identifier texture;
    private final Identifier secondLayer;
    private final Identifier thirdLayer;
    private final float distortionSpeed = 0.5f;
    private final float distortionSeparationFactor = 32f;
    private final float distortionFactor = 2;
    private final float scale = 32f;
    private float speed = 4f;
    private float time = 0;

    public VortexBackgroundRenderer(Identifier texture) {
        this.texture = texture;
        String path = texture.getPath();
        String basePath = path.substring(0, path.length() - 4);
        this.secondLayer = new Identifier(texture.getNamespace(), basePath + "_second.png");
        this.thirdLayer = new Identifier(texture.getNamespace(), basePath + "_third.png");
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
//涡旋渲染位置控制
    public void render(MatrixStack matrixStack, int width, int height) {
        matrixStack.push();

        // 平移到屏幕中心
        matrixStack.translate(width / 2f, height / 2f, 0);

        // 自适应铺满：按最大边等比缩放
        float scale = Math.max(width, height) / 32f;
        matrixStack.scale(scale, scale, 1.0f);

        // 时间计算...
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            this.time = client.getTickDelta() + client.player.age;
        } else {
            this.time = (System.currentTimeMillis() % 100000) / 50f;
        }

        // 禁用深度测试，确保绘制在背景层
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        // 渲染三个图层
        this.renderLayer(matrixStack, 1.0F, texture);
        this.renderLayer(matrixStack, 1.5f, secondLayer);
        this.renderLayer(matrixStack, 2.5f, thirdLayer);

        // 恢复深度设置
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        matrixStack.pop();
    }

    private void renderLayer(MatrixStack matrixStack, float scaleFactor, Identifier layer) {
        if (MinecraftClient.getInstance().getResourceManager().getResource(layer).isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getRenderTypeBeaconBeamProgram);
        RenderSystem.setShaderTexture(0, layer);

        matrixStack.push();
        matrixStack.scale(scale / scaleFactor, scale / scaleFactor, scale);

        MinecraftClient.getInstance().getTextureManager().bindTexture(layer);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

        for (int i = 0; i < 32; ++i) {
            this.renderSection(buffer, i,
                    this.time / (120 / this.speed),
                    (float) Math.sin(i * Math.PI / 32),
                    (float) Math.sin((i + 1) * Math.PI / 32),
                    matrixStack.peek().getNormalMatrix(),
                    matrixStack.peek().getPositionMatrix());
        }

        tessellator.draw();
        matrixStack.pop();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void renderSection(VertexConsumer builder, int zOffset, float textureDistanceOffset,
                               float startScale, float endScale, Matrix3f normalMatrix, Matrix4f positionMatrix) {
        float panel = 1 / 6f;
        float sqrt = (float) Math.sqrt(3) / 2.0f;
        int vOffset = (zOffset * panel + textureDistanceOffset > 1.0) ? zOffset - 6 : zOffset;
        float distortion = computeDistortionFactor(time, zOffset);
        float distortionPlusOne = computeDistortionFactor(time, zOffset + 1);
        float panelDistanceOffset = panel + textureDistanceOffset;
        float vPanelOffset = (vOffset * panel) + textureDistanceOffset;

        int uOffset = 0;
        float uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, 0f, -startScale + distortion, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f, -endScale + distortionPlusOne, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * -sqrt, endScale / -2f + distortionPlusOne, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, startScale * -sqrt, startScale / -2f + distortion, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 1;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, startScale * -sqrt, startScale / -2f + distortion, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * -sqrt, endScale / -2f + distortionPlusOne, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * -sqrt, endScale / 2f + distortionPlusOne, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, startScale * -sqrt, startScale / 2f + distortion, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 2;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, 0f, endScale + distortionPlusOne, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f, startScale + distortion, -zOffset, uPanelOffset + panel, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, startScale * -sqrt, startScale / 2f + distortion, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * -sqrt, endScale / 2f + distortionPlusOne, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);

        uOffset = 3;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, 0f, startScale + distortion, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f, endScale + distortionPlusOne, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * sqrt, endScale / 2f + distortionPlusOne, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, startScale * sqrt, startScale / 2f + distortion, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 4;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, startScale * sqrt, startScale / 2f + distortion, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * sqrt, endScale / 2f + distortionPlusOne, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * sqrt, endScale / -2f + distortionPlusOne, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, startScale * sqrt, startScale / -2f + distortion, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 5;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, 0f, -endScale + distortionPlusOne, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f, -startScale + distortion, -zOffset, uPanelOffset + panel, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, startScale * sqrt, startScale / -2f + distortion, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, endScale * sqrt, endScale / -2f + distortionPlusOne, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
    }

    private void addVertex(VertexConsumer builder, Matrix3f normalMatrix, Matrix4f matrix,
                           float x, float y, float z, float u, float v) {
        builder.vertex(matrix, x, y, z)
                .color(1, 1, 1, 1f)
                .texture(u, v)
                .light(0xF000F0)
                .normal(normalMatrix, 0, 0, 0)
                .next();
    }

    private float computeDistortionFactor(float time, int t) {
        return (float) (Math.sin(time * this.distortionSpeed * 0.1 * Math.PI +
                (13 - t) * this.distortionSeparationFactor) * this.distortionFactor) / 6;
    }
}