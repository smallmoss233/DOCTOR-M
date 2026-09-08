package doctor_m.handler.KeytoTime;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_item.KeytoTimeFragment.RelicGemItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GemDeathSaveHandler {
    // 存储玩家无敌结束的游戏刻
    private static final ConcurrentHashMap<UUID, Long> invincibleMap = new ConcurrentHashMap<>();

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }

            var world = player.getWorld();
            long currentTick = world.getTime();

            // 1. 优先检查无敌状态
            Long endTick = invincibleMap.get(player.getUuid());
            if (endTick != null) {
                if (currentTick < endTick) {
                    // 无敌时间内，取消本次伤害
                    return false;
                } else {
                    // 已过期，清理
                    invincibleMap.remove(player.getUuid());
                }
            }

            // 2. 检测致命伤害
            if (player.getHealth() - amount > 0) {
                return true;
            }

            // 3. 查找装备的宝石
            Optional<TrinketComponent> componentOptional = TrinketsApi.getTrinketComponent(player);
            if (componentOptional.isEmpty()) {
                return true;
            }
            TrinketComponent component = componentOptional.get();

            List<Pair<SlotReference, ItemStack>> equipped = component.getEquipped(stack -> stack.getItem() instanceof RelicGemItem);
            if (equipped.isEmpty()) {
                return true;
            }
            ItemStack gemStack = equipped.get(0).getRight();

            // 4. 检查冷却
            long cooldownUntil = RelicGemItem.getCooldownUntilTick(gemStack);
            if (currentTick < cooldownUntil) {
                return true;
            }

            // 5. 触发复活
            int level = RelicGemItem.getLevel(gemStack);
            int activeTicks = RelicGemItem.getActiveTicks(level);
            int cooldownTicks = RelicGemItem.getCooldownTicks(level);
            int totalTicks = activeTicks + cooldownTicks;

            // 恢复满血
            player.setHealth(player.getMaxHealth());

            // 移除被动抗性（避免干扰）
            player.removeStatusEffect(StatusEffects.RESISTANCE);

            // 添加主动增益（速度、夜视、急迫、水下呼吸），不包含抗性
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, activeTicks, 2, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, activeTicks, 0, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, activeTicks, 1, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, activeTicks, 0, false, true, true));

            // 设置无敌时间（从当前刻开始，持续 activeTicks 刻）
            invincibleMap.put(player.getUuid(), currentTick + activeTicks);

            // 设置冷却（游戏刻）
            RelicGemItem.setCooldownUntilTick(gemStack, currentTick + totalTicks);

            // 特效 & 音效
            for (int i = 0; i < 60; i++) {
                double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.0;
                double y = player.getY() + player.getRandom().nextDouble() * 2.5;
                double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.0;
                player.getServerWorld().spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.2);
            }
            player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0f, 1.0f);

            // 取消本次致命伤害
            return false;
        });
    }
}