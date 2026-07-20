package doctor_m.mixin.ait_oxygenatedmixin;

import doctor_m.util.SpaceEnvironmentUtil;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "dev.amble.ait.module.planet.core.space.planet.Planet", remap = false)
public class MixinPlanetOxygenOverride {

    /**
     * @reason 全面接管 AIT 氧气判定，统一走 SpaceEnvironmentUtil
     */
    @Overwrite
    public static boolean hasOxygenInTank(LivingEntity entity) {
        return entity != null && SpaceEnvironmentUtil.hasBreathableAir(entity);
    }

    /**
     * @reason 统一全套判定
     */
    @Overwrite
    public static boolean hasFullSuit(LivingEntity entity) {
        return SpaceEnvironmentUtil.hasFullSpacesuit(entity);
    }
}