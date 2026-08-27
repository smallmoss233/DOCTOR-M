package doctor_m.mixin.client.doctor_m;

import doctor_m.module.creativity.creativity_data.Tlipoca.TlipocaScytheItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/ItemRenderer;renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/item/ItemStack;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void doctor_m$renderEmissive(ItemStack stack,
                                         net.minecraft.client.render.model.json.ModelTransformationMode renderMode,
                                         boolean leftHanded, MatrixStack matrices,
                                         VertexConsumerProvider vertexConsumers,
                                         int light, int overlay, BakedModel model,
                                         CallbackInfo ci) {
        if (!(stack.getItem() instanceof TlipocaScytheItem)) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEyes(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));

        Random random = Random.create();
        long seed = 42L;

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
        consumer.quad(matrices.peek(), emissiveQuad, 3f, 3f, 3f, 15728880, overlay);
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