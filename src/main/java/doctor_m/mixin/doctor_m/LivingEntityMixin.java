package doctor_m.mixin.doctor_m;

import doctor_m.Item.data_itme.ForceFieldShieldItem;
import doctor_m.Item.stcs.STCSItem;
import doctor_m.module.creativity.creativity_data.Tlipoca.TlipocaScytheItem;
import doctor_m.util.creativity.ScytheChargingManager;
import doctor_m.util.creativity.ScytheSlashManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    private static final ThreadLocal<Boolean> TLIPOCA_AOE_LOCK = ThreadLocal.withInitial(() -> false);
    private static final double TLIPOCA_AOE_RADIUS = 5.0;
    private static final ThreadLocal<Boolean> STCS_BLOCK_LOCK = ThreadLocal.withInitial(() -> false);

    // ========== 力场盾==========
    /** 只要正在举盾就生效（不需要有能量） */
    private static boolean isHoldingForceFieldShield(PlayerEntity player) {
        if (!player.isUsingItem()) return false;
        return player.getActiveItem().getItem() instanceof ForceFieldShieldItem;
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$onShieldDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;
        if (!isHoldingForceFieldShield(player)) return;
        if (ForceFieldShieldItem.isEnvironmentalOrSpecialDamage(source)) return;
        cir.setReturnValue(false);
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$reduceEnvironmentalDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return amount;
        if (!isHoldingForceFieldShield(player)) return amount;
        if (ForceFieldShieldItem.isEnvironmentalOrSpecialDamage(source)) {
            return amount * 0.1f;
        }
        return amount;
    }

    // ========== 特莉波卡镰刀 ===========

    /**
     * 镰刀蓄力期间完全无敌
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$chargingInvulnerable(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity player && ScytheChargingManager.isCharging(player)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isHoldingTlipocaScythe(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof TlipocaScytheItem
                || player.getOffHandStack().getItem() instanceof TlipocaScytheItem;
    }

    /**
     * 处决斩杀：低血量（≤35%）或伤害溢出（amount ≥ health）直接 setHealth(0)
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$tlipocaExecute(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (TLIPOCA_AOE_LOCK.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingTlipocaScythe(player)) return;

        // amount 已经过 ModifyVariable 修改（倍率后）
        if (self.getHealth() <= self.getMaxHealth() * 0.35f || amount >= self.getHealth()) {
            self.setHealth(0);
            // 提前 return 会导致 @At("RETURN") 不执行，手动补后效
            applyTlipocaPostEffects(self, player, amount, true);
            cir.setReturnValue(true);
        }
    }

    /**
     * 镰刀增伤：亡灵 ×5，其他 ×1.5
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$tlipocaModifyDamage(float amount, DamageSource source) {
        if (TLIPOCA_AOE_LOCK.get()) return amount;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return amount;
        if (!isHoldingTlipocaScythe(player)) return amount;

        if (self.getGroup() == EntityGroup.UNDEAD) {
            return amount * 5.0f;
        }
        return amount * 1.5f;
    }

    /**
     * 命中后：普通命中走这里；处决在 tlipocaExecute 已处理，不会重复（目标已死）
     */
    @Inject(method = "damage", at = @At("RETURN"))
    private void doctor_m$tlipocaPostDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TLIPOCA_AOE_LOCK.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingTlipocaScythe(player)) return;

        // 如果目标已死（伤害溢出），视为处决
        boolean isExecution = !self.isAlive();
        applyTlipocaPostEffects(self, player, amount, isExecution);
    }

    /**
     * 统一后效：吸血、饱食度、灵魂牵引、伤害共享、粒子
     */
    private static void applyTlipocaPostEffects(LivingEntity victim, PlayerEntity player,
                                                float amount, boolean isExecution) {

        // 每次攻击都生成红色扇形轨迹
        if (player.getWorld() instanceof ServerWorld world) {
            ScytheSlashManager.spawnSlashArcParticles(world, player);
        }

        // 恢复：处决 60%，普通 25%
        float healRatio = isExecution ? 0.6f : 0.25f;
        float heal = amount * healRatio;
        player.heal(heal);

        int food = Math.max(1, (int) heal);
        player.getHungerManager().add(food, food * 0.5f);

        if (!(victim.getWorld() instanceof ServerWorld world)) return;

        // 灵魂牵引：周围 3.5 格敌人拉向玩家
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

        // 伤害共享 / AoE（5格圆形）
        TLIPOCA_AOE_LOCK.set(true);
        try {
            double radiusSq = TLIPOCA_AOE_RADIUS * TLIPOCA_AOE_RADIUS;
            Box aoeBox = new Box(victim.getPos(), victim.getPos()).expand(TLIPOCA_AOE_RADIUS);

            for (Entity entity : world.getOtherEntities(victim, aoeBox)) {
                if (entity == player) continue;
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.squaredDistanceTo(victim) > radiusSq) continue;

                if (isExecution) {
                    // 处决：强制扣血（无视护甲、抗性、限伤）
                    float newHealth = living.getHealth() - amount;
                    living.setHealth(Math.max(0.0f, newHealth));
                } else {
                    // 普通：可被减免
                    living.damage(victim.getDamageSources().generic(), amount);
                }
            }
        } finally {
            TLIPOCA_AOE_LOCK.set(false);
        }

        // 命中粒子
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                victim.getX(), victim.getY() + victim.getHeight() * 0.6, victim.getZ(),
                5, 0.3, 0.2, 0.3, 0.0);

        // 处决额外灵魂特效
        if (isExecution) {
            world.spawnParticles(ParticleTypes.SOUL,
                    victim.getX(), victim.getY() + victim.getHeight() * 0.5, victim.getZ(),
                    15, 0.4, 0.4, 0.4, 0.06);
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    victim.getX(), victim.getY() + 0.3, victim.getZ(),
                    8, 0.3, 0.3, 0.3, 0.03);
        }
    }

    // ========== STCS 剑封锁（潜行触发） ==========

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$stcsBlock(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (STCS_BLOCK_LOCK.get()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;

        // 潜行才触发
        if (!player.isSneaking()) return;

        // 主手或副手持有 STCS 即可
        ItemStack stcsStack = null;
        STCSItem stcsItem = null;

        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();

        if (main.getItem() instanceof STCSItem s) {
            stcsStack = main;
            stcsItem = s;
        } else if (off.getItem() instanceof STCSItem s) {
            stcsStack = off;
            stcsItem = s;
        }

        if (stcsItem == null || stcsStack == null) return;

        // 能量不足：不格挡，不扣能量，静默失败
        int energy = stcsItem.getEnergy(stcsStack);
        if (energy < STCSItem.BLOCK_ENERGY_COST) return;

        stcsItem.addEnergy(stcsStack, -STCSItem.BLOCK_ENERGY_COST);

        float reduction = stcsItem.isCoreActive(stcsStack) ? 1.0f : stcsItem.getBlockDamageReduction();

        if (reduction >= 0.8f) spawnStcsBlockEffect(player);

        // 完全免疫
        if (reduction >= 0.999f) {
            cir.setReturnValue(false);
            return;
        }

        // 部分减伤
        float newAmount = amount * (1.0f - reduction);
        if (newAmount <= 0.01f) {
            cir.setReturnValue(false);
            return;
        }

        STCS_BLOCK_LOCK.set(true);
        try {
            player.damage(source, newAmount);
        } finally {
            STCS_BLOCK_LOCK.set(false);
        }
        cir.setReturnValue(false);
    }

    private static void spawnStcsBlockEffect(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        world.spawnParticles(ParticleTypes.CRIT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                8, 0.4, 0.4, 0.4, 0.3);
        world.spawnParticles(ParticleTypes.WAX_ON,
                player.getX(), player.getY() + 1.0, player.getZ(),
                4, 0.3, 0.3, 0.3, 0.1);
    }
}