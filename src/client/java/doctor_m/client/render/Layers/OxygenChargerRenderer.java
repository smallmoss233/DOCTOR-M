package doctor_m.client.render.Layers;

import doctor_m.block.entities.OxygenChargerBlockEntity;
import doctor_m.client.util.EmissiveRenderHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class OxygenChargerRenderer implements BlockEntityRenderer<OxygenChargerBlockEntity> {

    public OxygenChargerRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(OxygenChargerBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.getWorld() == null) return;

        BlockState state = entity.getCachedState();
        MinecraftClient client = MinecraftClient.getInstance();
        BakedModel model = client.getBlockRenderManager().getModel(state);

        // 2. 渲染发光层
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        EmissiveRenderHelper.renderEmissive(stack, model, matrices, vertexConsumers, light, overlay);
    }
}