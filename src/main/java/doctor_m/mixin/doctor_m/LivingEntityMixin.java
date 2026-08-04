package doctor_m.mixin.doctor_m;

import doctor_m.Item.data_itme.ForceFieldShieldItem;
import doctor_m.module.creativity.creativity_data.TlipocaScytheItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // 防止伤害共享递归
    private static final ThreadLocal<Boolean> TLIPOCA_AOE_LOCK = ThreadLocal.withInitial(() -> false);
    private static final double TLIPOCA_AOE_RADIUS = 4.0;

    // ========== 力场盾（完全保留）==========

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$onShieldDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;
        if (!ForceFieldShieldItem.isForceFieldActive(player)) return;
        if (ForceFieldShieldItem.isEnvironmentalOrSpecialDamage(source)) return;
        cir.setReturnValue(false);
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$reduceEnvironmentalDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return amount;
        if (!ForceFieldShieldItem.isForceFieldActive(player)) return amount;
        if (ForceFieldShieldItem.isEnvironmentalOrSpecialDamage(source)) {
            return amount * 0.1f;
        }
        return amount;
    }

    // ========== 特莉波卡镰刀 ===========

    private static boolean isHoldingTlipocaScythe(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof TlipocaScytheItem
                || player.getOffHandStack().getItem() instanceof TlipocaScytheItem;
    }

    /**
     * 处决斩杀：目标血量 ≤ 35% 直接 setHealth(0)，无视限伤
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$tlipocaExecute(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (TLIPOCA_AOE_LOCK.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingTlipocaScythe(player)) return;

        if (self.getHealth() <= self.getMaxHealth() * 0.35f) {
            self.setHealth(0);
            applyTlipocaEffects(self, player, amount, true);
            cir.setReturnValue(true);
        }
    }

    /**
     * 镰刀增伤：+50%
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$tlipocaModifyDamage(float amount, DamageSource source) {
        if (TLIPOCA_AOE_LOCK.get()) return amount;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return amount;
        if (!isHoldingTlipocaScythe(player)) return amount;
        return amount * 1.5f;
    }

    /**
     * 命中后：生命偷取 + 饱食度 + 灵魂牵引 + 伤害共享
     */
    @Inject(method = "damage", at = @At("RETURN"))
    private void doctor_m$tlipocaPostDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TLIPOCA_AOE_LOCK.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingTlipocaScythe(player)) return;

        applyTlipocaEffects(self, player, amount, !self.isAlive());
    }

    /**
     * 统一的后效处理（处决和普通命中共用）
     */
    private static void applyTlipocaEffects(LivingEntity victim, PlayerEntity player, float amount, boolean targetDied) {
        // 1. 生命偷取：25% 伤害转化为治疗
        float heal = amount * 0.25f;
        player.heal(heal);

        // 2. 恢复饱食度（饥饿值 = 治疗量取整，饱和度 = 一半）
        int food = Math.max(1, (int) heal);
        player.getHungerManager().add(food, food * 0.5f);

        if (!(victim.getWorld() instanceof ServerWorld world)) return;

        // 3. 灵魂牵引：周围 3.5 格敌人拉向玩家
        Box pullBox = new Box(victim.getPos(), victim.getPos()).expand(3.5);
        Vec3d playerPos = player.getPos();
        for (Entity entity : world.getOtherEntities(victim, pullBox)) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity)) continue;

            Vec3d diff = playerPos.subtract(entity.getPos());
            double distSq = diff.lengthSquared();
            if (distSq < 0.01 || distSq > 12.25) continue;

            Vec3d dir = diff.normalize();
            double strength = 0.35 * (1.0 - Math.sqrt(distSq) / 3.5);

            entity.setVelocity(entity.getVelocity().add(
                    dir.x * strength,
                    0.08 + strength * 0.2,
                    dir.z * strength
            ));
            entity.velocityDirty = true;
        }

        // 4. 伤害共享 / AoE
        TLIPOCA_AOE_LOCK.set(true);
        try {
            double radiusSq = TLIPOCA_AOE_RADIUS * TLIPOCA_AOE_RADIUS;
            Box aoeBox = new Box(victim.getPos(), victim.getPos()).expand(TLIPOCA_AOE_RADIUS);

            for (Entity entity : world.getOtherEntities(victim, aoeBox)) {
                if (entity == player) continue; // 排除使用者
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.squaredDistanceTo(victim) > radiusSq) continue;

                if (targetDied) {
                    // 强制扣血（无视护甲、抗性、限伤）
                    float newHealth = living.getHealth() - amount;
                    living.setHealth(Math.max(0.0f, newHealth));
                } else {
                    // 普通伤害（可被减免，用 generic 防止递归）
                    living.damage(victim.getDamageSources().generic(), amount);
                }
            }
        } finally {
            TLIPOCA_AOE_LOCK.set(false);
        }

        // 5. 命中粒子
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                victim.getX(), victim.getY() + victim.getHeight() * 0.6, victim.getZ(),
                5, 0.3, 0.2, 0.3, 0.0);
    }
}