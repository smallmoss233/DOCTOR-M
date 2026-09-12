package doctor_m.client.render;

import doctor_m.client.util.EmissiveRenderHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/**
 * 通用方块实体发光层渲染器。
 *
 * <p>无需针对每个 BlockEntity 写单独的渲染器：只要方块有对应的
 * {@code xxx_emissive.png} 纹理，就可以用本渲染器为其方块实体渲染发光层。
 *
 * <p>使用方式：
 * <pre>{@code
 * BlockEntityRendererRegistry.register(ModBlockEntities.FOO, EmissiveBlockEntityRenderer::new);
 * BlockEntityRendererRegistry.register(ModBlockEntities.BAR, EmissiveBlockEntityRenderer::new);
 * }</pre>
 */
public class EmissiveBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    public EmissiveBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.getWorld() == null) return;

        BlockState state = entity.getCachedState();
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel model = client.getBlockRenderManager().getModel(state);

        ItemStack stack = new ItemStack(state.getBlock().asItem());
        EmissiveRenderHelper.renderEmissive(stack, model, matrices, vertexConsumers, light, overlay);
    }
}