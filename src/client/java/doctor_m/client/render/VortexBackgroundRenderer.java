package doctor_m.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class VortexBackgroundRenderer {
    private static VortexBackgroundRenderer INSTANCE;

    public Identifier texture;
    public Identifier secondLayer;
    public Identifier thirdLayer;
    private final float distortionSpeed;
    private final float distortionSeparationFactor;
    private final float distortionFactor;
    private final float scale;
    private float speed = 4f;
    private float time = 0;

    public VortexBackgroundRenderer(Identifier texture) {
        replaceWith(texture);
        this.distortionSpeed = 0.5f;
        this.distortionSeparationFactor = 32f;
        this.distortionFactor = 2;
        this.scale = 32f;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public static VortexBackgroundRenderer getInstance(Identifier texture) {
        if (INSTANCE == null) {
            INSTANCE = new VortexBackgroundRenderer(texture);
        } else if (!INSTANCE.isFor(texture)) {
            INSTANCE.replaceWith(texture);
        }
        return INSTANCE;
    }

    public static VortexBackgroundRenderer getCurrentInstance(Identifier fallback) {
        if (INSTANCE == null) {
            INSTANCE = new VortexBackgroundRenderer(fallback);
        }
        return INSTANCE;
    }

    public static VortexBackgroundRenderer getCurrentInstance() {
        return INSTANCE;
    }

    public boolean isFor(Identifier texture) {
        return this.texture.equals(texture);
    }

    public void replaceWith(Identifier texture) {
        this.texture = texture;
        String basePath = texture.getPath();
        if (basePath.endsWith(".png")) {
            basePath = basePath.substring(0, basePath.length() - 4);
        }
        this.secondLayer = new Identifier(texture.getNamespace(), basePath + "_second.png");
        this.thirdLayer = new Identifier(texture.getNamespace(), basePath + "_third.png");
    }

    /**
     * 屏幕空间入口：保留你的自适应与空安全
     */
    public void render(MatrixStack matrixStack, int width, int height) {
        matrixStack.push();

        matrixStack.translate(width / 2f, height / 2f, 0);
        float screenScale = Math.max(width, height) / 32f;
        matrixStack.scale(screenScale, screenScale, 1.0f);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            this.time = client.getTickDelta() + client.player.age;
        } else {
            this.time = (System.currentTimeMillis() % 100000) / 50f;
        }

        this.renderLayer(matrixStack, 1.0F, texture);
        this.renderLayer(matrixStack, 1.5f);
        this.renderLayer(matrixStack, 2.5f);

        matrixStack.pop();
    }

    public void renderLayer(MatrixStack matrixStack, float scaleFactor) {
        Identifier currentTexture = scaleFactor == 1.5f ? secondLayer : thirdLayer;
        if (MinecraftClient.getInstance().getResourceManager().getResource(currentTexture).isEmpty()) return;
        this.renderLayer(matrixStack, scaleFactor, currentTexture);
    }

    /**
     * 核心修复：Z 轴压缩 + 禁用深度测试 + 反向绘制
     */
    private void renderLayer(MatrixStack matrixStack, float scaleFactor, Identifier layer) {
        if (MinecraftClient.getInstance().getResourceManager().getResource(layer).isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();   // 避免 GUI 深度冲突与穿模
        RenderSystem.depthMask(false);      // 不污染深度缓冲

        RenderSystem.setShader(GameRenderer::getRenderTypeBeaconBeamProgram);
        RenderSystem.setShaderTexture(0, layer);

        matrixStack.push();
        // Z 轴缩到 0.01f：保留极小的深度值用于层级，但彻底避开远平面裁剪
        matrixStack.scale(scale / scaleFactor, scale / scaleFactor, 0.01f);

        MinecraftClient.getInstance().getTextureManager().bindTexture(layer);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

        // 从远到近绘制（i = 31 → 0），禁用深度测试后靠画家算法保证正确遮挡
        for (int i = 31; i >= 0; --i) {
            this.renderSection(
                    buffer, i,
                    this.time / (120 / this.speed),
                    (float) Math.sin(i * Math.PI / 32),
                    (float) Math.sin((i + 1) * Math.PI / 32),
                    matrixStack.peek().getNormalMatrix(),
                    matrixStack.peek().getPositionMatrix()
            );
        }

        tessellator.draw();
        matrixStack.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * 核心修复：在顶点级别手动做透视除法，让正交投影下的屏幕空间也能有强烈 3D 隧道感
     */
    private void renderSection(VertexConsumer builder, int zOffset, float textureDistanceOffset,
                               float startScale, float endScale, Matrix3f normalMatrix, Matrix4f positionMatrix) {
        float panel = 1 / 6f;
        float sqrt = (float) Math.sqrt(3) / 2.0f;
        int vOffset = (zOffset * panel + textureDistanceOffset > 1.0) ? zOffset - 6 : zOffset;
        float distortion = computeDistortionFactor(time, zOffset);
        float distortionPlusOne = computeDistortionFactor(time, zOffset + 1);
        float panelDistanceOffset = panel + textureDistanceOffset;
        float vPanelOffset = (vOffset * panel) + textureDistanceOffset;

        // 手动透视参数：focalLength 越小，隧道感越强；推荐 40~72
        float focalLength = 56f;
        float pStart = focalLength / (focalLength + zOffset);      // 近端截面（Z = -zOffset）
        float pEnd   = focalLength / (focalLength + zOffset + 1);  // 远端截面（Z = -zOffset-1）

        int uOffset = 0;
        float uPanelOffset = uOffset * panel;

        // 所有 X、Y 都乘上对应 Z 深度的透视因子，远处自动缩小
        addVertex(builder, normalMatrix, positionMatrix, 0f * pStart, (-startScale + distortion) * pStart, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f * pEnd, (-endScale + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * -sqrt) * pEnd, (endScale / -2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (startScale * -sqrt) * pStart, (startScale / -2f + distortion) * pStart, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 1;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, (startScale * -sqrt) * pStart, (startScale / -2f + distortion) * pStart, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * -sqrt) * pEnd, (endScale / -2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * -sqrt) * pEnd, (endScale / 2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (startScale * -sqrt) * pStart, (startScale / 2f + distortion) * pStart, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 2;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, 0f * pEnd, (endScale + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f * pStart, (startScale + distortion) * pStart, -zOffset, uPanelOffset + panel, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, (startScale * -sqrt) * pStart, (startScale / 2f + distortion) * pStart, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * -sqrt) * pEnd, (endScale / 2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);

        uOffset = 3;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, 0f * pStart, (startScale + distortion) * pStart, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f * pEnd, (endScale + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * sqrt) * pEnd, (endScale / 2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (startScale * sqrt) * pStart, (startScale / 2f + distortion) * pStart, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 4;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, (startScale * sqrt) * pStart, (startScale / 2f + distortion) * pStart, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * sqrt) * pEnd, (endScale / 2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * sqrt) * pEnd, (endScale / -2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, (startScale * sqrt) * pStart, (startScale / -2f + distortion) * pStart, -zOffset, uPanelOffset + panel, vPanelOffset);

        uOffset = 5;
        uPanelOffset = uOffset * panel;
        addVertex(builder, normalMatrix, positionMatrix, 0f * pEnd, (-endScale + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset + panel, vOffset * panel + panelDistanceOffset);
        addVertex(builder, normalMatrix, positionMatrix, 0f * pStart, (-startScale + distortion) * pStart, -zOffset, uPanelOffset + panel, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, (startScale * sqrt) * pStart, (startScale / -2f + distortion) * pStart, -zOffset, uPanelOffset, vPanelOffset);
        addVertex(builder, normalMatrix, positionMatrix, (endScale * sqrt) * pEnd, (endScale / -2f + distortionPlusOne) * pEnd, -zOffset - 1, uPanelOffset, vOffset * panel + panelDistanceOffset);
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