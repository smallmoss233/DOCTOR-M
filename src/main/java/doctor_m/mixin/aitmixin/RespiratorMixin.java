package doctor_m.mixin.aitmixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.AITTags;

@Mixin(LivingEntity.class)
public class RespiratorMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void doctor_m$respiratorTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 不在水里不处理
        if (!self.isSubmergedIn(FluidTags.WATER)) return;

        ItemStack head = self.getEquippedStack(EquipmentSlot.HEAD);
        boolean hasRespirator = head.isIn(AITTags.Items.FULL_RESPIRATORS)
                || head.isIn(AITTags.Items.HALF_RESPIRATORS);

        if (!hasRespirator) return;

        // 有水下呼吸药水就不额外补，防止叠加成无限氧气
        if (self.hasStatusEffect(StatusEffects.WATER_BREATHING)) return;

        // 循环呼吸器：每 3 tick 补 1 点空气，减缓约 33% 流失
        if (self.age % 3 == 0 && self.getAir() < self.getMaxAir()) {
            self.setAir(Math.min(self.getAir() + 1, self.getMaxAir()));
        }
    }
}