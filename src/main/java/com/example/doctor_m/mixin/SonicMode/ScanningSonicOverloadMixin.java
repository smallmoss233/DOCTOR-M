package com.example.doctor_m.mixin.SonicMode;

import net.minecraft.advancement.Advancement;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.amble.ait.core.AITSounds;
import dev.amble.ait.core.item.sonic.ScanningSonicMode;
import dev.amble.ait.core.item.sonic.SonicMode;

@Mixin(ScanningSonicMode.class)
public abstract class ScanningSonicOverloadMixin extends SonicMode {

    private static final Identifier TARGET_ENTITY = new Identifier("doctor_m", "type_103w_evereye");
    private static final Identifier OVERLOAD_ADVANCEMENT = new Identifier("doctor_m", "scan_overload");
    private static final int COOLDOWN_TICKS = 60;

    protected ScanningSonicOverloadMixin(int index) {
        super(index);
    }

    @Inject(
            method = "process",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onProcessHead(ItemStack stack, World world, PlayerEntity user,
                               CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

        if (player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            cir.setReturnValue(false);
            return;
        }

        HitResult hitResult = SonicMode.getHitResult(user);
        if (!(hitResult instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity)) return;

        // 获取实体类型的 Identifier
        Identifier entityId = EntityType.getId(entityHit.getEntity().getType());
        if (entityId != null && entityId.equals(TARGET_ENTITY)) {
            triggerOverload(serverWorld, player, stack, entityHit.getEntity());
            cir.setReturnValue(false);
        }
    }

    private void triggerOverload(ServerWorld world, ServerPlayerEntity player, ItemStack stack, net.minecraft.entity.Entity target) {
        // 1. 大量火花粒子（在目标实体位置）
        double x = target.getX();
        double y = target.getY() + target.getHeight() / 2;
        double z = target.getZ();
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 50, 1.0, 0.5, 1.0, 0.2);
        world.spawnParticles(ParticleTypes.SONIC_BOOM, x, y, z, 30, 0.8, 0.5, 0.8, 0.1);
        world.spawnParticles(ParticleTypes.FLAME, x, y, z, 20, 0.5, 0.3, 0.5, 0.05);

        // 2. 故障音效
        world.playSound(null, player.getBlockPos(), AITSounds.SONIC_TWEAK, SoundCategory.PLAYERS, 2.0f, 0.1f);
        world.playSound(null, player.getBlockPos(), AITSounds.SONIC_SWITCH, SoundCategory.PLAYERS, 2.0f, 2.0f);

        // 3. 触发成就（替代发送消息）
        grantOverloadAdvancement(player);
        player.sendMessage(
                Text.translatable("tooltip.doctor_m.scan.overload").formatted(Formatting.RED, Formatting.BOLD),
                false
        );

        // 4. 起子冷却
        player.getItemCooldownManager().set(stack.getItem(), COOLDOWN_TICKS);
    }

    private void grantOverloadAdvancement(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement advancement = server.getAdvancementLoader().get(OVERLOAD_ADVANCEMENT);
        if (advancement == null) return;

        player.getAdvancementTracker().grantCriterion(advancement, "overload");
    }
}