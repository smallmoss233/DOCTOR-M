package com.example.doctor_m.mixin;  // 根据你的实际包名调整

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import doctor_m.Item.data_itme.time_key;
import doctor_m.wolrd_data.TimeKeyDamageHandler;

@Mixin(MobEntity.class)
public class MobEntityMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity player) {
            boolean hasTimeKey = TrinketsApi.getTrinketComponent(player)
                    .map(component -> component.isEquipped(stack -> stack.getItem() instanceof time_key))
                    .orElse(false);
            // 从我们的 Map 中读取状态
            boolean neutralEnabled = TimeKeyDamageHandler.neutralMode.getOrDefault(player.getUuid(), false);
            if (hasTimeKey && neutralEnabled) {
                ci.cancel();
            }
        }
    }
}