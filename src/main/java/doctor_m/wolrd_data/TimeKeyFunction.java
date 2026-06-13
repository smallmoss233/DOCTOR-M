package doctor_m.wolrd_data;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.GameMode;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import doctor_m.Item.data_itme.time_key;

public class TimeKeyFunction {

    private static final ThreadLocal<Boolean> isCustomDamage = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Long> revivalCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 24000; // 1游戏日
    private static final Map<UUID, GameMode> lastGameMode = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHealTime = new ConcurrentHashMap<>();

    public static void register() {
        // 伤害限制 + 复活
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (isCustomDamage.get()) {
                isCustomDamage.set(false);
                return true;
            }
            if (entity instanceof ServerPlayerEntity player) {
                boolean hasTimeKey = isTimeKeyEquipped(player);
                if (hasTimeKey) {
                    // 伤害限制：不超过最大生命值的15%
                    float maxHealth = player.getMaxHealth();
                    float maxAllowed = maxHealth * 0.15f;
                    float newAmount = Math.min(amount, maxAllowed);
                    float newHealth = player.getHealth() - newAmount;

                    // 复活
                    if (newHealth <= 0 && !isInCooldown(player)) {
                        revivePlayer(player);
                        revivalCooldown.put(player.getUuid(), player.getServerWorld().getTime() + COOLDOWN_TICKS);
                        return false;
                    }

                    if (newAmount != amount) {
                        isCustomDamage.set(true);
                        player.damage(source, newAmount);
                        return false;
                    }
                }
            }
            return true;
        });

        // 2. 潜行右键切换强制中立模式（状态存储在时间钥匙物品的NBT中）
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (player.isSneaking() && stack.getItem() instanceof time_key) {
                NbtCompound nbt = stack.getOrCreateNbt();
                boolean current = nbt.getBoolean("neutral_mode");
                nbt.putBoolean("neutral_mode", !current);
                player.sendMessage(Text.translatable("message.doctor_m.time_key.neutral_mode." + (!current ? "on" : "off")), true);
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });

        // 3. 每秒恢复10%生命值
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getTicks();
            for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!isTimeKeyEquipped(player)) continue;
                Long last = lastHealTime.get(player.getUuid());
                if (last == null || now - last >= 20) {
                    float healAmount = player.getMaxHealth() * 0.1f;
                    player.heal(healAmount);
                    lastHealTime.put(player.getUuid(), now);
                }
            }
        });

        // 4. 游戏模式切换检测，确保从创造/旁观切换回生存时恢复飞行
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                GameMode current = player.interactionManager.getGameMode();
                GameMode previous = lastGameMode.get(player.getUuid());
                if (previous != null && previous != current) {
                    if ((previous == GameMode.CREATIVE || previous == GameMode.SPECTATOR) &&
                            (current == GameMode.SURVIVAL || current == GameMode.ADVENTURE)) {
                        if (isTimeKeyEquipped(player)) {
                            if (!player.getAbilities().allowFlying) {
                                player.getAbilities().allowFlying = true;
                                player.sendAbilitiesUpdate();
                            }
                        }
                    }
                }
                lastGameMode.put(player.getUuid(), current);
            }
        });
    }

    private static boolean isTimeKeyEquipped(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(comp -> comp.isEquipped(stack -> stack.getItem() instanceof time_key))
                .orElse(false);
    }

    private static boolean isInCooldown(ServerPlayerEntity player) {
        Long cooldownUntil = revivalCooldown.get(player.getUuid());
        if (cooldownUntil == null) return false;
        return player.getServerWorld().getTime() < cooldownUntil;
    }

    private static void revivePlayer(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth());
        player.clearStatusEffects();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false));

        // 复活时清除周围敌对生物（半径10格）
        double radius = 10.0;
        player.getServerWorld().getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class,
                player.getBoundingBox().expand(radius),
                entity -> entity != player && entity.isAlive() && (entity instanceof HostileEntity)
        ).forEach(net.minecraft.entity.LivingEntity::kill);

        // 复活的粒子效果
        for (int i = 0; i < 50; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.0;
            double y = player.getY() + player.getRandom().nextDouble() * 2.0;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.0;
            player.getServerWorld().spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.1);
            player.getServerWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0.05);
        }
        player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
        player.sendMessage(Text.translatable("message.doctor_m.time_key_resurrection"), true);
        // 复活重新开启飞行能力
        if (!player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
    }
}