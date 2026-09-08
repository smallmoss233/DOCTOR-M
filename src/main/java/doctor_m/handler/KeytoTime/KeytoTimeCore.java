package doctor_m.handler.KeytoTime;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry;
import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_item.KeytoTimeItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KeytoTimeCore {

    private static final ThreadLocal<Boolean> customDamage = ThreadLocal.withInitial(() -> false);
    private static final ConcurrentHashMap<UUID, GameMode> lastGameMode = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastHealTime = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> protectionEndTime = new ConcurrentHashMap<>();

    private static final Set<String> VANILLA_DAMAGE_TYPES = Set.of(
            "inFire", "onFire", "lava", "hotFloor", "inWall", "cramming",
            "drown", "starve", "cactus", "fall", "flyIntoWall", "outOfWorld",
            "generic", "magic", "indirectMagic", "dragonBreath", "wither",
            "anvil", "fallingStalactite", "stalagmite", "lightningBolt", "freeze",
            "sonicBoom", "outsideBorder", "genericKill", "dryout", "sweetBerryBush",
            "fallingBlock", "trident", "arrow", "mob", "player", "explosion",
            "fireworks", "fireball", "witherSkull", "thrown", "sting", "badRespawnPoint"
    );

    private static void ensureMaxHealth(ServerPlayerEntity player) {
        var attr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attr != null && attr.getValue() <= 0.0) {
            attr.setBaseValue(20.0);
        }
    }

    public static ItemStack getTimeKeyStack(PlayerEntity player) {
        try {
            return getTimeKeyStackUnsafe(player);
        } catch (NullPointerException e) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack getTimeKeyStackUnsafe(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof KeytoTimeItem) return mainHand;

        ItemStack offHand = player.getOffHandStack();
        if (offHand.getItem() instanceof KeytoTimeItem) return offHand;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof KeytoTimeItem) return stack;
        }

        try {
            var componentOpt = TrinketsApi.getTrinketComponent(player);
            if (componentOpt.isPresent()) {
                var list = componentOpt.get().getEquipped(stack -> stack.getItem() instanceof KeytoTimeItem);
                if (!list.isEmpty()) {
                    return list.get(0).getRight();
                }
            }
        } catch (Throwable ignored) {
        }
        return ItemStack.EMPTY;
    }

    public static boolean isTimeKeyEquipped(PlayerEntity player) {
        return !getTimeKeyStack(player).isEmpty();
    }

    public static void onDeathIntercepted(ServerPlayerEntity player) {
        protectionEndTime.put(player.getUuid(), player.getServer().getTicks() + 2400L);
    }

    public static void clearProtection(ServerPlayerEntity player) {
        protectionEndTime.remove(player.getUuid());
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (customDamage.get()) {
                customDamage.set(false);
                return true;
            }

            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }

            if (!isTimeKeyEquipped(player)) return true;

            ensureMaxHealth(player);
            if (KeytoTimePassive.isGodMode(player)) {
                player.setHealth(player.getMaxHealth());
                return false;
            }

            if (!VANILLA_DAMAGE_TYPES.contains(source.getName())) {
                return false;
            }

            Long protectionEnd = protectionEndTime.get(player.getUuid());
            if (protectionEnd != null) {
                if (player.getServer().getTicks() <= protectionEnd) {
                    ensureMaxHealth(player);
                    player.setHealth(player.getMaxHealth());
                    return false;
                }
            }

            if (source.getSource() instanceof ProjectileEntity proj && proj.getOwner() != player) {
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.5f, 1.5f);
                for (int i = 0; i < 10; i++) {
                    player.getServerWorld().spawnParticles(ParticleTypes.CLOUD,
                            player.getX() + (player.getRandom().nextDouble() - 0.5),
                            player.getY() + player.getRandom().nextDouble(),
                            player.getZ() + (player.getRandom().nextDouble() - 0.5),
                            1, 0.0, 0.0, 0.0, 0.0);
                }
                proj.discard();
                return false;
            }

            switch (source.getName()) {
                case "inFire", "onFire", "lava", "magic", "indirectMagic", "wither", "drown",
                     "starve", "fall", "cactus", "hotFloor", "sweetBerryBush", "freeze",
                     "inWall", "lightningBolt", "thorns", "sonicBoom", "outOfWorld",
                     "dryout", "stalagmite", "fallingStalactite", "cramming", "flyIntoWall",
                     "generic" -> {
                    return false;
                }
            }

            float maxAllowed = player.getMaxHealth() * 0.15f;
            float newAmount = Math.min(amount, maxAllowed);
            float newHealth = player.getHealth() - newAmount;

            if (newHealth <= 0) {
                ensureMaxHealth(player);
                player.setHealth(player.getMaxHealth());
                onDeathIntercepted(player);
                return false;
            }

            if (newAmount != amount) {
                customDamage.set(true);
                player.damage(source, newAmount);
                return false;
            }

            return true;
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (isTimeKeyEquipped(player)) {
                    ensureMaxHealth(player);
                    revivePlayer(player);
                    return false;
                }
            }
            return true;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getTicks();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                boolean hasTimeKey = isTimeKeyEquipped(player);
                boolean godMode = hasTimeKey && KeytoTimePassive.isGodMode(player);

                Long protectionEnd = protectionEndTime.get(player.getUuid());
                if (protectionEnd != null) {
                    if (now <= protectionEnd) {
                        player.getServerWorld().getEntitiesByClass(
                                LivingEntity.class,
                                player.getBoundingBox().expand(3.0),
                                e -> e != player && e.isAlive()
                        ).forEach(target -> eraseTargetDeMatStyle(target, player, player.getServerWorld()));
                    } else {
                        protectionEndTime.remove(player.getUuid());
                    }
                }

                if (hasTimeKey) {
                    float healAmount = player.getMaxHealth() * 0.1f;
                    Long lastHeal = lastHealTime.get(player.getUuid());
                    if (lastHeal != null) {
                        if (now - lastHeal >= 20) {
                            player.heal(healAmount);
                            lastHealTime.put(player.getUuid(), now);
                        }
                    } else {
                        player.heal(healAmount);
                        lastHealTime.put(player.getUuid(), now);
                    }

                    var hunger = player.getHungerManager();
                    int foodAdd = (int) healAmount;
                    int newFood = Math.min(hunger.getFoodLevel() + foodAdd, 20);
                    float newSaturation = Math.min(hunger.getSaturationLevel() + healAmount, newFood);
                    hunger.setFoodLevel(newFood);
                    hunger.setSaturationLevel(newSaturation);

                    if (player.isOnFire()) {
                        player.setFireTicks(0);
                        player.setOnFire(false);
                    }
                }

                if (godMode) {
                    ensureMaxHealth(player);
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                    if (player.isOnFire()) {
                        player.setFireTicks(0);
                        player.setOnFire(false);
                    }
                    player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
                    player.removeStatusEffect(StatusEffects.WITHER);
                    if (player.getAir() < player.getMaxAir()) player.setAir(player.getMaxAir());
                    if (player.getHungerManager().getFoodLevel() < 20) {
                        player.getHungerManager().setFoodLevel(20);
                        player.getHungerManager().setSaturationLevel(20f);
                    }
                    if (!player.isAlive() || player.getHealth() <= 0) {
                        ensureMaxHealth(player);
                        revivePlayer(player);
                    }
                }

                GameMode current = player.interactionManager.getGameMode();
                GameMode previous = lastGameMode.get(player.getUuid());
                if (previous != null && previous != current) {
                    if ((previous == GameMode.CREATIVE || previous == GameMode.SPECTATOR) &&
                            (current == GameMode.SURVIVAL || current == GameMode.ADVENTURE) &&
                            hasTimeKey && !player.getAbilities().allowFlying) {
                        player.getAbilities().allowFlying = true;
                        player.sendAbilitiesUpdate();
                    }
                }
                lastGameMode.put(player.getUuid(), current);

                if (hasTimeKey) {
                    var world = player.getWorld();
                    var planet = (Object) null; // placeholder to avoid import if not needed
                    try {
                        planet = PlanetRegistry.getInstance().get(world);
                    } catch (Exception ignored) {
                    }
                    boolean worldHasOxygen = planet == null || ((dev.amble.ait.module.planet.core.space.planet.Planet) planet).hasOxygen();

                    if (!worldHasOxygen) {
                        if (!player.hasStatusEffect(AITStatusEffects.OXYGENATED)) {
                            player.addStatusEffect(new StatusEffectInstance(AITStatusEffects.OXYGENATED, 60, 0, false, false));
                        } else {
                            var effect = player.getStatusEffect(AITStatusEffects.OXYGENATED);
                            if (effect != null && effect.getDuration() < 40) {
                                player.addStatusEffect(new StatusEffectInstance(AITStatusEffects.OXYGENATED, 60, 0, false, false));
                            }
                        }
                    }
                }
            }
        });

        KeytoTimePassive.registerAttackCallback();
    }

    private static void eraseTargetDeMatStyle(LivingEntity target, ServerPlayerEntity player, ServerWorld world) {
        var pos = target.getPos();

        for (int i = 0; i < 20; i++) {
            world.spawnParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.5, pos.z, 1,
                    (world.random.nextDouble() - 0.5) * 0.8,
                    (world.random.nextDouble() - 0.5) * 0.8,
                    (world.random.nextDouble() - 0.5) * 0.8, 0.05);
        }
        for (int i = 0; i < 10; i++) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x, pos.y + 0.3, pos.z, 1,
                    (world.random.nextDouble() - 0.5) * 0.5,
                    world.random.nextDouble() * 0.5,
                    (world.random.nextDouble() - 0.5) * 0.5, 0.02);
        }

        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.25f, 1.8f);

        if (target instanceof ServerPlayerEntity) {
            target.kill();
        } else {
            target.setHealth(1f);
            DamageSource source = player.getDamageSources().playerAttack(player);
            boolean died = target.damage(source, 999f);

            if (!died && target.isAlive()) {
                target.setHealth(0f);
                try {
                    Method onDeath = LivingEntity.class.getDeclaredMethod("onDeath", DamageSource.class);
                    onDeath.setAccessible(true);
                    onDeath.invoke(target, source);
                } catch (Exception e) {
                    target.kill();
                }
            }
        }
    }

    public static void revivePlayer(ServerPlayerEntity p) {
        if (p.getWorld() == null) return;

        ensureMaxHealth(p);
        onDeathIntercepted(p);

        try {
            Class<LivingEntity> clazz = LivingEntity.class;
            Field deathTime = clazz.getDeclaredField("deathTime");
            deathTime.setAccessible(true);
            deathTime.set(p, 0);

            Field hurtTime = clazz.getDeclaredField("hurtTime");
            hurtTime.setAccessible(true);
            hurtTime.set(p, 0);

            Field fallDistance = clazz.getDeclaredField("fallDistance");
            fallDistance.setAccessible(true);
            fallDistance.set(p, 0f);
        } catch (Exception ignored) {
        }

        p.setHealth(p.getMaxHealth());
        p.clearStatusEffects();
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 2, false, false));

        p.getServerWorld().getEntitiesByClass(
                LivingEntity.class,
                p.getBoundingBox().expand(35.0),
                e -> e != p && e.isAlive() && e instanceof HostileEntity
        ).forEach(e -> e.kill());

        for (int i = 0; i < 50; i++) {
            p.getServerWorld().spawnParticles(ParticleTypes.END_ROD,
                    p.getX() + (p.getRandom().nextDouble() - 0.5) * 2.0,
                    p.getY() + p.getRandom().nextDouble() * 2.0,
                    p.getZ() + (p.getRandom().nextDouble() - 0.5) * 2.0,
                    1, 0.0, 0.0, 0.0, 0.1);
            p.getServerWorld().spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    p.getX() + (p.getRandom().nextDouble() - 0.5) * 2.0,
                    p.getY() + p.getRandom().nextDouble() * 2.0,
                    p.getZ() + (p.getRandom().nextDouble() - 0.5) * 2.0,
                    1, 0.0, 0.0, 0.0, 0.05);
        }

        p.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1f, 1f);
        p.sendMessage(Text.translatable("message.doctor_m.key_to_time_resurrection"), true);

        if (!p.getAbilities().allowFlying) {
            p.getAbilities().allowFlying = true;
            p.sendAbilitiesUpdate();
        }
    }
}