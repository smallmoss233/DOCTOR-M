package doctor_m.mixin.ait_oxygenatedmixin;

import doctor_m.util.SpaceEnvironmentUtil;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "dev.amble.ait.core.effects.OxygenatedEffect", remap = false)
public class MixinOxygenatedEffect {

    /**
     * @reason 统一判定入口，避免与 Planet.hasOxygenInTank 逻辑分叉
     */
    @Overwrite
    public static boolean isOxygenated(LivingEntity entity) {
        return entity != null && SpaceEnvironmentUtil.hasBreathableAir(entity);
    }
}