package doctor_m.mixin.doctor_m;

import doctor_m.handler.TimeKey.TimeKeyFunction;
import doctor_m.handler.TimeKey.TimeKeyPassive;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class AbsoluteProtectionMixin {

    // ===== GodMode：damage 完全免疫，方法头直接 return false =====
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockGodModeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && TimeKeyPassive.isGodMode(player)) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            cir.setReturnValue(false);
        }
    }

    // ===== 拦截 kill()（/kill 命令、代码调用）=====
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockKill(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                player.setHealth(player.getMaxHealth());
                TimeKeyFunction.onDeathIntercepted(player); // 触发保护期
                ci.cancel();
            }
        }
    }

    // ===== 拦截 setHealth（GodMode 强制满血，非 GodMode 防归零）=====
    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$blockHealthSet(float health) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            // GodMode：任何 setHealth 都强制满血，连"受伤"的概念都不存在
            if (TimeKeyPassive.isGodMode(player)) {
                return player.getMaxHealth();
            }
            // 非 GodMode：防死亡 + 触发保护期
            if (health <= 0.0f && TimeKeyFunction.isTimeKeyEquipped(player)) {
                TimeKeyFunction.onDeathIntercepted(player);
                return player.getMaxHealth();
            }
        }
        return health;
    }

    // ===== 拦截 onDeath（死亡回调，防止死亡逻辑执行）=====
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockOnDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                TimeKeyFunction.revivePlayer(player);
                ci.cancel();
            }
        }
    }

    // ===== Tick 兜底：health <= 0 或 deathTime > 0 立即拉起来 =====
    @Inject(method = "tick", at = @At("HEAD"))
    private void doctor_m$tickResurrection(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                if (player.getHealth() <= 0.0f || player.deathTime > 0) {
                    TimeKeyFunction.revivePlayer(player);
                }
            }
        }
    }
}