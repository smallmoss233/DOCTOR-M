package doctor_m.module.creativity.creativity_data.Tlipoca;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class TlipocaScytheEvents {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(TlipocaScytheEvents::onDeath);
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingScythe(player)) return;

        if (entity.getWorld() instanceof ServerWorld world) {
            // 灵魂粒子爆发
            world.spawnParticles(ParticleTypes.SOUL,
                    entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ(),
                    20, 0.4, 0.4, 0.4, 0.06);

            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    entity.getX(), entity.getY() + 0.3, entity.getZ(),
                    10, 0.3, 0.3, 0.3, 0.03);

            // 死神低鸣
            world.playSound(null, entity.getBlockPos(),
                    SoundEvents.ENTITY_WITHER_DEATH,
                    SoundCategory.PLAYERS, 0.6f, 0.6f);
        }
    }

    public static boolean isHoldingScythe(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof TlipocaScytheItem
                || player.getOffHandStack().getItem() instanceof TlipocaScytheItem;
    }
}