package doctor_m.client.render.TrinketRenderer;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import doctor_m.Item.items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class SCTrinketRenderer implements TrinketRenderer {

    public static void register() {
        TrinketRendererRegistry.registerRenderer(items.SHIELD_CORE, new SCTrinketRenderer());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(ItemStack stack, SlotReference slotReference,
                       EntityModel<? extends LivingEntity> contextModel,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {

        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        if (!(contextModel instanceof PlayerEntityModel)) return;

        PlayerEntityModel<AbstractClientPlayerEntity> playerModel =
                (PlayerEntityModel<AbstractClientPlayerEntity>) contextModel;

        boolean isSlim = "slim".equals(player.getModel());

        matrices.push();

        // 定位到胸部，然后向下移动至腰部，并向后偏移（挂在腰后）
        TrinketRenderer.translateToChest(matrices, playerModel, player);
        matrices.translate(0.0, 0.2, 0.45);

        if (isSlim) {
            matrices.scale(1f, 1f, 1f);
        } else {
            matrices.scale(1.0f, 1.0f, 1.0f);
        }

        // 旋转物品使其正面朝外，避免上下颠倒
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        // 如果还需要调整朝向，可再绕Y轴旋转
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));

        MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack,
                ModelTransformationMode.FIXED,
                light, OverlayTexture.DEFAULT_UV,
                matrices, vertexConsumers,
                entity.getWorld(), 0
        );

        matrices.pop();
    }
}