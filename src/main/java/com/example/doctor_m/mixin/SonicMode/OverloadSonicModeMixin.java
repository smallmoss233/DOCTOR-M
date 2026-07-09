package com.example.doctor_m.mixin.SonicMode;

import net.minecraft.advancement.Advancement;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.item.sonic.OverloadSonicMode;
import dev.amble.ait.core.item.sonic.SonicMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(OverloadSonicMode.class)
public abstract class OverloadSonicModeMixin extends SonicMode {

    private static final Identifier DEAFENING_CHALLENGE = new Identifier("doctor_m", "deafening");
    private static final int INSTANT_DAMAGE = 50;
    private static final int STUN_DURATION_TICKS = 10 * 20;
    private static final int COOLDOWN_TICKS = 15 * 20;

    private static final Map<ServerPlayerEntity, Boolean> hasStunned = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    protected OverloadSonicModeMixin(int index) {
        super(index);
    }

    @Inject(
            method = "process",
            at = @At("TAIL"),
            cancellable = false
    )
    private void onProcessTail(ServerWorld world, LivingEntity user, int ticks, CallbackInfo ci) {
        if (!(user instanceof ServerPlayerEntity player)) return;

        if (player.getItemCooldownManager().isCoolingDown(player.getMainHandStack().getItem())) {
            return;
        }

        HitResult entityHitResult = SonicMode.getHitResult(user);
        if (!(entityHitResult instanceof EntityHitResult entityHit)) return;

        if (!(entityHit.getEntity() instanceof WardenEntity warden)) return;

        if (hasStunned.getOrDefault(player, false)) {
            return;
        }

        // 造成伤害
        float currentHealth = warden.getHealth();
        float newHealth = currentHealth - INSTANT_DAMAGE;
        warden.setHealth(Math.max(0, newHealth));

        // 硬控：禁用 AI
        warden.setAiDisabled(true);
        // 10 秒后恢复 AI
        final WardenEntity finalWarden = warden;
        final ServerWorld finalWorld = world;
        SCHEDULER.schedule(() -> {
            finalWorld.getServer().execute(() -> {
                if (!finalWarden.isRemoved() && finalWarden.isAlive()) {
                    finalWarden.setAiDisabled(false);
                }
            });
        }, STUN_DURATION_TICKS / 20, TimeUnit.SECONDS);

        // 播放音效
        world.playSound(
                null,
                warden.getBlockPos(),
                AITSounds.SONIC_TWEAK,
                SoundCategory.PLAYERS,
                3.0f,
                10.0f
        );

        // 粒子效果
        world.spawnParticles(
                net.minecraft.particle.ParticleTypes.SONIC_BOOM,
                warden.getX(), warden.getY() + 1.0, warden.getZ(),
                20, 0.5, 0.5, 0.5, 0.1
        );
        world.spawnParticles(
                net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                warden.getX(), warden.getY() + 1.0, warden.getZ(),
                30, 0.5, 0.5, 0.5, 0.1
        );

        // 触发成就
        grantDeafeningChallenge(player);

        // 起子冷却 15 秒
        ItemStack mainHand = player.getMainHandStack();
        player.getItemCooldownManager().set(mainHand.getItem(), COOLDOWN_TICKS);

        hasStunned.put(player, true);
        final ServerPlayerEntity finalPlayer = player;
        SCHEDULER.schedule(() -> {
            finalWorld.getServer().execute(() -> {
                hasStunned.remove(finalPlayer);
            });
        }, COOLDOWN_TICKS / 20, TimeUnit.SECONDS);
    }

    private void grantDeafeningChallenge(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement advancement = server.getAdvancementLoader().get(DEAFENING_CHALLENGE);
        if (advancement == null) return;

        player.getAdvancementTracker().grantCriterion(advancement, "deafening");
    }
}