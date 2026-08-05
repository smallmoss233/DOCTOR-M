package doctor_m.mixin.client.doctor_m;

import doctor_m.module.creativity.creativity_data.TlipocaScytheItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class ScytheFirstPersonMixin {

    // 客户端渲染单线程，静态字段存原始 swingProgress 就够了
    private static float doctor_m$rawSwing = 0.0F;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void doctor_m$capture(
            AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item,
            float equipProgress, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light,
            CallbackInfo ci
    ) {
        doctor_m$rawSwing = swingProgress;
    }

    // 1. 清零 swingProgress，取消原版向上挥的旋转
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    private float doctor_m$cancelSwing(
            float value,
            AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item,
            float equipProgress, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light
    ) {
        if (item.getItem() instanceof TlipocaScytheItem && doctor_m$rawSwing > 0.0F) {
            return 0.0F;
        }
        return value;
    }

    // 2. 攻击时把 equipProgress 设为 1 - rawSwing，让手臂像切换物品一样从下方切入
    @ModifyVariable(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            ordinal = 3,  // equipProgress 是第 4 个 float 参数
            argsOnly = true
    )
    private float doctor_m$equipLikeSwing(
            float value,
            AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item,
            float equipProgress, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light
    ) {
        if (item.getItem() instanceof TlipocaScytheItem && doctor_m$rawSwing > 0.0F) {
            return 1.0F - doctor_m$rawSwing;
        }
        return value;
    }
}