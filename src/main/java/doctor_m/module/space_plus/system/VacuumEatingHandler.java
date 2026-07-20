package doctor_m.module.space_plus.system;

import dev.amble.ait.core.AITStatusEffects;
import doctor_m.util.SpaceEnvironmentUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VacuumEatingHandler {

    private static final Map<UUID, Long> LAST_DAMAGE_TIME = new HashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();
    private static final long DAMAGE_COOLDOWN_MS = 1000;
    private static final long MESSAGE_COOLDOWN_MS = 3000;

    public static void register() {
        UseItemCallback.EVENT.register(VacuumEatingHandler::onUseItem);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            LAST_DAMAGE_TIME.remove(uuid);
            LAST_MESSAGE_TIME.remove(uuid);
        });
    }

    private static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, net.minecraft.util.Hand hand) {
        if (world.isClient()) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        ItemStack stack = serverPlayer.getStackInHand(hand);
        UseAction action = stack.getUseAction();
        if (action != UseAction.EAT && action != UseAction.DRINK) {
            return TypedActionResult.pass(stack);
        }

        // ========== 双重保险 ==========
        // 保险1：只要有氧气效果（氧气机/宇航服），直接放行，不扣任何东西
        if (serverPlayer.hasStatusEffect(AITStatusEffects.OXYGENATED)) {
            return TypedActionResult.pass(stack);
        }

        // 保险2：环境本身有氧（主世界、TARDIS 等）
        if (SpaceEnvironmentUtil.hasEnvironmentalOxygen(serverPlayer)) {
            return TypedActionResult.pass(stack);
        }

        if (serverPlayer.isCreative()) {
            return TypedActionResult.pass(stack);
        }

        punish(serverPlayer);
        return TypedActionResult.fail(stack);
    }

    private static void punish(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUuid();

        // 氧气泄漏（每次必扣）
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem) {
            double current = OxygenSystem.getOxygen(chest);
            if (current > 0) {
                int leak = 20 + player.getRandom().nextInt(31);
                double remaining = Math.max(0.0, current - leak);
                OxygenSystem.setOxygen(chest, remaining);

                if (remaining == 0.0 && current > 0) {
                }
            }
        }

        // 伤害（带冷却）
        Long lastDmg = LAST_DAMAGE_TIME.get(uuid);
        if (lastDmg == null || now - lastDmg > DAMAGE_COOLDOWN_MS) {
            LAST_DAMAGE_TIME.put(uuid, now);
            player.damage(player.getDamageSources().dryOut(), 4.0f);
        }

        // 提示（带冷却）
        Long lastMsg = LAST_MESSAGE_TIME.get(uuid);
        if (lastMsg == null || now - lastMsg > MESSAGE_COOLDOWN_MS) {
            LAST_MESSAGE_TIME.put(uuid, now);
            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.vacuum_consuming")
                            .formatted(Formatting.RED),
                    true
            );
        }
    }
}