package doctor_m.mixin.doctor_m;

import doctor_m.world_data.TimeKey.TimeKeyFunction;
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

    /**
     * 拦截 damage() HEAD：GodMode 玩家直接返回 false，跳过整个伤害计算。
     * 普通装备者走 ALLOW_DAMAGE 事件，这里不拦截。
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockGodModeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            if (TimeKeyFunction.isGodMode(player)) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * 拦截 kill()：任何直接调用 kill() 的斩杀（包括命令、模组逻辑）全部无效。
     * 所有装备时间钥匙的玩家都生效。
     */
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void doctor_m$blockKill(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                player.setHealth(player.getMaxHealth());
                ci.cancel();
            }
        }
    }

    /**
     * 拦截 setHealth(0)：把传入的 0 偷换成 maxHealth。
     * 这是最深层的保险，连 SelfDestructHandler 的 setHealth(0) 三连也会变成回血。
     * 所有装备时间钥匙的玩家都生效。
     */
    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$blockHealthSet(float health) {
        if ((Object) this instanceof ServerPlayerEntity player && health <= 0.0f) {
            if (TimeKeyFunction.isTimeKeyEquipped(player)) {
                return player.getMaxHealth();
            }
        }
        return health;
    }
}