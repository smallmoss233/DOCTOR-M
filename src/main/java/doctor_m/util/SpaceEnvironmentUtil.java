package doctor_m.util;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry;
import doctor_m.module.space_plus.system.SpaceOxygenManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class SpaceEnvironmentUtil {

    /**
     * 综合判定：生物当前是否能呼吸（用于进食、被动能力等外部系统）
     * 包含宇航服供氧、氧气机、环境自然有氧
     */
    public static boolean hasBreathableAir(LivingEntity entity) {
        // 氧气机效果
        if (entity.hasStatusEffect(AITStatusEffects.OXYGENATED)) return true;

        // 宇航服供氧
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem) {
            if (SpaceOxygenManager.getOxygen(chest) > 0) return true;
        }

        // 环境自然有氧
        return hasEnvironmentalOxygen(entity);
    }

    /**
     * 仅判定环境本身是否有氧（用于宇航服 tick：决定消不消耗氧气）
     * 不算宇航服，不算生物自身状态
     */
    public static boolean hasEnvironmentalOxygen(LivingEntity entity) {
        World world = entity.getWorld();
        if (world == null) return true;

        // 氧气机效果（属于环境供氧）
        if (entity.hasStatusEffect(AITStatusEffects.OXYGENATED)) return true;

        // 星球环境
        boolean worldHasOxygen = true;
        try {
            var planet = PlanetRegistry.getInstance().get(world);
            if (planet != null) {
                worldHasOxygen = planet.hasOxygen();
            }
        } catch (Exception ignored) {}

        boolean isTardis = world.getRegistryKey().getValue().getNamespace().equals("ait")
                && world.getRegistryKey().getValue().getPath().startsWith("tardis");

        boolean isHeadInsideBlock = !world.getBlockState(entity.getBlockPos().up(1)).isAir();

        return (worldHasOxygen || isTardis) && !isHeadInsideBlock;
    }

    public static boolean isInVacuum(LivingEntity entity) {
        return !hasBreathableAir(entity);
    }

    public static boolean hasFullSpacesuit(LivingEntity entity) {
        if (entity == null) return false;
        return entity.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem
                && entity.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem
                && entity.getEquippedStack(EquipmentSlot.LEGS).getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem
                && entity.getEquippedStack(EquipmentSlot.FEET).getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem;
    }
}