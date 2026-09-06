package doctor_m.client.util;

import doctor_m.client.render.DOCTORMMRenderLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class EmissiveRenderHelper {

    public static void renderEmissive(ItemStack stack, BakedModel model, MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers, int light, int overlay) {
        VertexConsumer consumer = vertexConsumers.getBuffer(
                DOCTORMMRenderLayers.tardisEmissiveCullZOffset(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));

        Random random = Random.create();
        final long seed = 42L;

        for (Direction direction : Direction.values()) {
            random.setSeed(seed);
            List<BakedQuad> quads = model.getQuads(null, direction, random);
            for (BakedQuad quad : quads) {
                renderEmissiveQuad(matrices, consumer, quad, overlay);
            }
        }

        random.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(null, null, random);
        for (BakedQuad quad : quads) {
            renderEmissiveQuad(matrices, consumer, quad, overlay);
        }
    }

    private static void renderEmissiveQuad(MatrixStack matrices, VertexConsumer consumer,
                                           BakedQuad quad, int overlay) {
        Sprite emissive = findEmissive(quad.getSprite());
        if (emissive == null) return;

        BakedQuad emissiveQuad = remapQuad(quad, emissive);
        consumer.quad(matrices.peek(), emissiveQuad, 2f, 2f, 2f, 15728880, overlay);
    }

    private static Sprite findEmissive(Sprite base) {
        SpriteContents contents = base.getContents();
        if (contents == null) return null;

        Identifier id = contents.getId();
        if (id.getPath().endsWith("_emissive")) return null;

        Identifier emissiveId = new Identifier(id.getNamespace(), id.getPath() + "_emissive");

        Sprite emissive = MinecraftClient.getInstance()
                .getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
                .apply(emissiveId);

        if (emissive == null || emissive.getContents() == null) return null;
        if (!emissive.getContents().getId().equals(emissiveId)) return null;

        return emissive;
    }

    private static BakedQuad remapQuad(BakedQuad quad, Sprite emissive) {
        int[] data = quad.getVertexData().clone();
        Sprite base = quad.getSprite();

        for (int i = 0; i < 4; i++) {
            int uIdx = i * 8 + 4;
            int vIdx = i * 8 + 5;

            float oldU = Float.intBitsToFloat(data[uIdx]);
            float oldV = Float.intBitsToFloat(data[vIdx]);

            float baseWidth = base.getMaxU() - base.getMinU();
            float baseHeight = base.getMaxV() - base.getMinV();
            float emissiveWidth = emissive.getMaxU() - emissive.getMinU();
            float emissiveHeight = emissive.getMaxV() - emissive.getMinV();

            if (baseWidth <= 0 || baseHeight <= 0) continue;

            float uNorm = (oldU - base.getMinU()) / baseWidth;
            float vNorm = (oldV - base.getMinV()) / baseHeight;

            data[uIdx] = Float.floatToRawIntBits(emissive.getMinU() + uNorm * emissiveWidth);
            data[vIdx] = Float.floatToRawIntBits(emissive.getMinV() + vNorm * emissiveHeight);
        }

        return new BakedQuad(data, quad.getColorIndex(), quad.getFace(), emissive, quad.hasShade());
    }
}