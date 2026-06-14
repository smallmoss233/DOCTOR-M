package doctor_m.wolrd_data;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.world.GameMode;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import doctor_m.Item.data_itme.time_key;

public class TimeKeyFunction {

    private static final ThreadLocal<Boolean> isCustomDamage = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Long> revivalCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 200;
    private static final Map<UUID, GameMode> lastGameMode = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHealTime = new ConcurrentHashMap<>();

    public static void register() {
        // ===================== 1. 伤害处理 =====================
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (isCustomDamage.get()) {
                isCustomDamage.set(false);
                return true;
            }
            if (entity instanceof ServerPlayerEntity player) {
                // 检查是否装备时间钥匙
                boolean hasTimeKey = isTimeKeyEquipped(player);
                if (hasTimeKey) {

                    //弹开箭矢
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

                    // 获取时间钥匙物品的 NBT（用于完全免疫开关）
                    ItemStack timeKeyStack = getTimeKeyStack(player);
                    boolean godmode = timeKeyStack.getOrCreateNbt().getBoolean("godmode");

                    // 完全免疫模式（免疫一切伤害，包括/kill）
                    if (godmode) {
                        return false;
                    }

                    // ---------- 部分免疫（常规负面伤害）----------
                    String name = source.getName();
                    if (name.equals("inFire") || name.equals("onFire") || name.equals("lava")
                            || name.equals("magic") || name.equals("indirectMagic")
                            || name.equals("wither") || name.equals("drown")
                            || name.equals("starve") || name.equals("fall")
                            || name.equals("cactus") || name.equals("hotFloor")
                            || name.equals("sweetBerryBush") || name.equals("freeze")
                            || name.equals("inWall") || name.equals("lightningBolt")
                            || name.equals("thorns") || name.equals("sonic_boom")
                            || name.equals("outOfWorld") || name.equals("dryout")
                            || name.equals("stalagmite") || name.equals("fallingStalactite")
                            || name.equals("cramming") || name.equals("flyIntoWall")
                            || name.equals("generic")) {
                        return false;
                    }

                    // 伤害限制：不超过最大生命值的 15%
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
// ===================== 3. 组合键+命令切换完全免疫 + 强制中立 =====================
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("passive")
                    .then(CommandManager.literal("a")
                            .executes(context -> {
                                PlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ItemStack stack = getTimeKeyStack(player);
                                if (stack.getItem() instanceof time_key) {
                                    NbtCompound nbt = stack.getOrCreateNbt();
                                    boolean current = nbt.getBoolean("neutral_mode");
                                    nbt.putBoolean("neutral_mode", !current);
                                    player.sendMessage(Text.translatable("message.doctor_m.time_key.neutral_mode." + (!current ? "on" : "off")), true);
                                } else {
                                    player.sendMessage(Text.translatable("message.doctor_m.time_key.not_equipped"), true);
                                }
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("b")
                            .executes(context -> {
                                PlayerEntity player = context.getSource().getPlayer();
                                if (player == null) return 0;
                                ItemStack stack = getTimeKeyStack(player);
                                if (stack.getItem() instanceof time_key) {
                                    NbtCompound nbt = stack.getOrCreateNbt();
                                    boolean current = nbt.getBoolean("godmode");
                                    nbt.putBoolean("godmode", !current);
                                    player.sendMessage(Text.translatable("message.doctor_m.time_key.godmode." + (!current ? "on" : "off")), true);
                                } else {
                                    player.sendMessage(Text.translatable("message.doctor_m.time_key.not_equipped"), true);
                                }
                                return 1;
                            })
                    )
            );
        });

        // ===================== 4. 生命恢复（每秒10%最大生命）+灭火 =====================
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getTicks();
            for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!isTimeKeyEquipped(player)) continue;
                Long last = lastHealTime.get(player.getUuid());
                if (last == null || now - last >= 20) {
                    player.heal(player.getMaxHealth() * 0.1f);
                    lastHealTime.put(player.getUuid(), now);
                }
                if (player.isOnFire()) {
                    player.setFireTicks(0);
                    player.setOnFire(false);
                }
            }
        });

        // ===================== 5. 游戏模式切换后恢复飞行 =====================
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                GameMode current = player.interactionManager.getGameMode();
                GameMode previous = lastGameMode.get(player.getUuid());
                if (previous != null && previous != current) {
                    if ((previous == GameMode.CREATIVE || previous == GameMode.SPECTATOR) &&
                            (current == GameMode.SURVIVAL || current == GameMode.ADVENTURE)) {
                        if (isTimeKeyEquipped(player) && !player.getAbilities().allowFlying) {
                            player.getAbilities().allowFlying = true;
                            player.sendAbilitiesUpdate();
                        }
                    }
                }
                lastGameMode.put(player.getUuid(), current);
            }
        });
    }

    private static ItemStack getTimeKeyStack(PlayerEntity player) {
        // 检查主手
        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof time_key) return main;
        // 检查饰品栏（Trinkets）
        return TrinketsApi.getTrinketComponent(player)
                .flatMap(comp -> comp.getEquipped(stack -> stack.getItem() instanceof time_key).stream().findFirst())
                .map(Pair::getRight)
                .orElse(ItemStack.EMPTY);
    }

    private static boolean isTimeKeyEquipped(PlayerEntity player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(comp -> comp.isEquipped(stack -> stack.getItem() instanceof time_key))
                .orElse(false);
    }

    private static boolean isInCooldown(ServerPlayerEntity player) {
        Long cooldownUntil = revivalCooldown.get(player.getUuid());
        return cooldownUntil != null && player.getServerWorld().getTime() < cooldownUntil;
    }

    private static void revivePlayer(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth());
        player.clearStatusEffects();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false));

        double radius = 35.0;
        player.getServerWorld().getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class,
                player.getBoundingBox().expand(radius),
                entity -> entity != player && entity.isAlive() && (entity instanceof HostileEntity)
        ).forEach(net.minecraft.entity.LivingEntity::kill);

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