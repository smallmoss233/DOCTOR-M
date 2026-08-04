package doctor_m.client.render;

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

public class VMTrinketRenderer implements TrinketRenderer {

    public static void register() {
        TrinketRendererRegistry.registerRenderer(items.VORTEX_MANIPULATOR, new VMTrinketRenderer());
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

        TrinketRenderer.translateToRightArm(matrices, playerModel, player);

        if (isSlim) {
            matrices.translate(0.03, -0.1, -0.12);
            matrices.scale(1f, 1f, 1f);
        } else {
            matrices.translate(0.0, -0.1, -0.12);
            matrices.scale(1.1f, 1.1f, 1.1f);
        }

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));

        MinecraftClient.getInstance().getItemRenderer().renderItem(
                stack,
                ModelTransformationMode.THIRD_PERSON_RIGHT_HAND,
                light, OverlayTexture.DEFAULT_UV,
                matrices, vertexConsumers,
                entity.getWorld(), 0
        );

        matrices.pop();
    }
}