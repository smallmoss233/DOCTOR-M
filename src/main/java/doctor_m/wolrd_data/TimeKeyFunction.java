package doctor_m.wolrd_data;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import doctor_m.Item.data_itme.time_key;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimeKeyFunction {

    public static final Map<UUID, Boolean> neutralMode = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastToggleTime = new ConcurrentHashMap<>();
    private static final long TOGGLE_COOLDOWN_TICKS = 5; // 5 tick = 0.25秒
    private static final ThreadLocal<Boolean> isCustomDamage = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Long> revivalCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHealTime = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 200; // 10秒复活冷却

    public static void register() {
        // 伤害限制 + 弹开箭矢 + 复活
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (isCustomDamage.get()) {
                isCustomDamage.set(false);
                return true;
            }
            if (entity instanceof ServerPlayerEntity player) {
                boolean hasTimeKey = isTimeKeyEquipped(player);
                if (hasTimeKey) {
                    // 弹开箭矢（非玩家射出的）
                    if (source.getSource() instanceof PersistentProjectileEntity projectile && projectile.getOwner() != player) {
                        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.5f, 1.5f);
                        for (int i = 0; i < 10; i++) {
                            double x = projectile.getX() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                            double y = projectile.getY() + player.getRandom().nextDouble() * 1.0;
                            double z = projectile.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                            player.getServerWorld().spawnParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0, 0, 0);
                        }
                        projectile.discard();
                        return false;
                    }

                    // 伤害限制 20% 最大生命
                    float maxHealth = player.getMaxHealth();
                    float maxAllowed = maxHealth * 0.15f;
                    float newAmount = Math.min(amount, maxAllowed);
                    float newHealth = player.getHealth() - newAmount;

                    // 致命伤害复活
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

        // 每秒恢复 10% 生命值
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

        // 右键切换强制中立模式（潜行右键）
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (player.isSneaking() && stack.getItem() instanceof time_key) {
                UUID uuid = player.getUuid();
                long now = world.getTime();
                Long last = lastToggleTime.get(uuid);
                if (last != null && now - last < TOGGLE_COOLDOWN_TICKS) {
                    return TypedActionResult.pass(stack); // 冷却中，不处理
                }
                boolean current = neutralMode.getOrDefault(uuid, false);
                neutralMode.put(uuid, !current);
                lastToggleTime.put(uuid, now);
                player.sendMessage(Text.translatable("message.doctor_m.time_key.neutral_mode." + (!current ? "on" : "off")), true);
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static boolean isTimeKeyEquipped(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(comp -> comp.isEquipped(stack -> stack.getItem() instanceof time_key))
                .orElse(false);
    }

    private static boolean isInCooldown(PlayerEntity player) {
        Long cooldownUntil = revivalCooldown.get(player.getUuid());
        if (cooldownUntil == null) return false;
        return player.getEntityWorld().getTime() < cooldownUntil;
    }

    private static void revivePlayer(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false));
        double radius = 10.0;
        player.getServerWorld().getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(radius),
                entity -> entity != player && entity.isAlive() && (entity instanceof net.minecraft.entity.mob.HostileEntity)
        ).forEach(LivingEntity::kill);
        player.clearStatusEffects();
        // 自定义复活粒子
        for (int i = 0; i < 50; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.0;
            double y = player.getY() + player.getRandom().nextDouble() * 2.0;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.0;
            player.getServerWorld().spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.1);
            player.getServerWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0.05);
        }
        player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
        player.sendMessage(Text.translatable("message.doctor_m.time_key_resurrection"), true);
        if (!player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
    }
}