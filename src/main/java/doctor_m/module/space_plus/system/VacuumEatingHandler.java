package doctor_m.module.space_plus.system;

import doctor_m.util.SpaceEnvironmentUtil;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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

    // 冷却表：防止玩家连点右键刷伤害/刷消息
    private static final Map<UUID, Long> LAST_DAMAGE_TIME = new HashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();
    private static final long DAMAGE_COOLDOWN_MS = 1000;   // 1秒一次伤害
    private static final long MESSAGE_COOLDOWN_MS = 3000;  // 3秒一次提示

    public static void register() {
        UseItemCallback.EVENT.register(VacuumEatingHandler::onUseItem);

        // 玩家下线时清理，防止内存泄漏
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            LAST_DAMAGE_TIME.remove(uuid);
            LAST_MESSAGE_TIME.remove(uuid);
        });
    }

    private static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, net.minecraft.util.Hand hand) {
        // 客户端不管
        if (world.isClient()) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        // 只处理服务端玩家
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        ItemStack stack = serverPlayer.getStackInHand(hand);
        UseAction action = stack.getUseAction();

        // 只拦截吃/喝
        if (action != UseAction.EAT && action != UseAction.DRINK) {
            return TypedActionResult.pass(stack);
        }

        // 环境有氧：放行
        if (SpaceEnvironmentUtil.hasEnvironmentalOxygen(serverPlayer)) {
            return TypedActionResult.pass(stack);
        }

        // 创造模式：放行（或者你想连创造都阻止也可以删掉这行）
        if (serverPlayer.isCreative()) {
            return TypedActionResult.pass(stack);
        }

        // ========== 真空进食：阻止并惩罚 ==========
        punish(serverPlayer);
        return TypedActionResult.fail(stack);  // ← 关键：阻止使用，不是 pass
    }

    private static void punish(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUuid();

        // 伤害（带冷却，防止连点刷血）
        Long lastDmg = LAST_DAMAGE_TIME.get(uuid);
        if (lastDmg == null || now - lastDmg > DAMAGE_COOLDOWN_MS) {
            LAST_DAMAGE_TIME.put(uuid, now);
            // 窒息/干燥伤害比 generic 更符合"真空暴露"
            player.damage(player.getDamageSources().dryOut(), 4.0f);
        }

        // 提示（带冷却，防止刷屏）
        Long lastMsg = LAST_MESSAGE_TIME.get(uuid);
        if (lastMsg == null || now - lastMsg > MESSAGE_COOLDOWN_MS) {
            LAST_MESSAGE_TIME.put(uuid, now);
            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.vacuum_consuming")
                            .formatted(Formatting.RED),
                    true  // action bar
            );
        }
    }
}