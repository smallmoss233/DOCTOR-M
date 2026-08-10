package doctor_m.module.space_plus;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.config.ConfigManager;
import doctor_m.util.SpaceEnvironmentUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VacuumEatingHandler {

    // 标记玩家"这次进食如果成功需要扣氧"
    private static final Map<UUID, Long> PENDING_EAT = new HashMap<>();

    public static void register() {
        UseItemCallback.EVENT.register(VacuumEatingHandler::onUseItem);
    }

    private static double getCost() {
        return ConfigManager.getConfig().vacuumEatingOxygenCost;
    }

    private static long getPendingTimeoutMs() {
        return ConfigManager.getConfig().vacuumEatingPendingTimeoutSeconds * 1000L;
    }

    private static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, Hand hand) {
        if (world.isClient()) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        ItemStack stack = serverPlayer.getStackInHand(hand);
        if (stack.getUseAction() != UseAction.EAT && stack.getUseAction() != UseAction.DRINK) {
            return TypedActionResult.pass(stack);
        }

        if (serverPlayer.hasStatusEffect(AITStatusEffects.OXYGENATED)
                || SpaceEnvironmentUtil.hasEnvironmentalOxygen(serverPlayer)
                || serverPlayer.isCreative()) {
            return TypedActionResult.pass(stack);
        }

        PENDING_EAT.put(serverPlayer.getUuid(), System.currentTimeMillis());
        return TypedActionResult.pass(stack);
    }

    public static void onPlayerActuallyEat(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Long pendingTime = PENDING_EAT.remove(uuid);
        if (pendingTime == null) return;

        if (System.currentTimeMillis() - pendingTime > getPendingTimeoutMs()) {
            return;
        }

        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof SpacesuitItem) {
            double current = OxygenSystem.getOxygen(chest);
            if (current > 0) {
                double remaining = Math.max(0.0, current - getCost());
                OxygenSystem.setOxygen(chest, remaining);
            }
        }
    }
}