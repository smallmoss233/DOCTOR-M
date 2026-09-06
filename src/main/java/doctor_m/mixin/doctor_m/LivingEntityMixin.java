package doctor_m.mixin.doctor_m;

import doctor_m.Item.data_item.ForceFieldShieldItem;
import doctor_m.Item.stcs.STCSItem;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
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

    private static final ModConfig CONFIG = ConfigManager.getConfig();

    private static final ThreadLocal<Boolean> TLIPOCA_AOE_LOCK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> STCS_BLOCK_LOCK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> STCS_AOE_LOCK = ThreadLocal.withInitial(() -> false);

    // ========== 力场盾==========
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

        if (CONFIG.forceFieldBlockAllNonEnvironmental) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float doctor_m$reduceEnvironmentalDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return amount;
        if (!isHoldingForceFieldShield(player)) return amount;
        if (ForceFieldShieldItem.isEnvironmentalOrSpecialDamage(source)) {
            return amount * (float) CONFIG.forceFieldEnvironmentalDamageMultiplier;
        }
        return amount;
    }

    // ========== 特莉波卡镰刀 ==========

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

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$tlipocaExecute(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (TLIPOCA_AOE_LOCK.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingTlipocaScythe(player)) return;

        if (self.getHealth() <= self.getMaxHealth() * 0.35f || amount >= self.getHealth()) {
            self.setHealth(0);
            applyTlipocaPostEffects(self, player, amount, true);
            cir.setReturnValue(true);
        }
    }

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

    @Inject(method = "damage", at = @At("RETURN"))
    private void doctor_m$tlipocaPostDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (TLIPOCA_AOE_LOCK.get()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingTlipocaScythe(player)) return;

        boolean isExecution = !self.isAlive();
        applyTlipocaPostEffects(self, player, amount, isExecution);
    }

    private static void applyTlipocaPostEffects(LivingEntity victim, PlayerEntity player,
                                                float amount, boolean isExecution) {

        if (player.getWorld() instanceof ServerWorld world) {
            ScytheSlashManager.spawnSlashArcParticles(world, player);
        }

        float healRatio = isExecution ? (float) CONFIG.tlipocaScytheExecuteHealRatio : (float) CONFIG.tlipocaScytheNormalHealRatio;
        float heal = amount * healRatio;
        player.heal(heal);

        int food = Math.max(CONFIG.tlipocaScytheFoodBase, (int) heal);
        player.getHungerManager().add(food, food * (float) CONFIG.tlipocaScytheSaturationMultiplier);

        if (!(victim.getWorld() instanceof ServerWorld world)) return;

        // 灵魂牵引
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

        // AoE
        TLIPOCA_AOE_LOCK.set(true);
        try {
            double radius = CONFIG.tlipocaScytheAoeRadius;
            double radiusSq = radius * radius;
            Box aoeBox = new Box(victim.getPos(), victim.getPos()).expand(radius);

            for (Entity entity : world.getOtherEntities(victim, aoeBox)) {
                if (entity == player) continue;
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.squaredDistanceTo(victim) > radiusSq) continue;

                if (isExecution) {
                    if (CONFIG.tlipocaScytheExecuteAoEDamageIgnoresArmor) {
                        float newHealth = living.getHealth() - amount;
                        living.setHealth(Math.max(0.0f, newHealth));
                    } else {
                        living.damage(victim.getDamageSources().generic(), amount);
                    }
                } else {
                    living.damage(victim.getDamageSources().generic(), amount);
                }
            }
        } finally {
            TLIPOCA_AOE_LOCK.set(false);
        }

        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                victim.getX(), victim.getY() + victim.getHeight() * 0.6, victim.getZ(),
                5, 0.3, 0.2, 0.3, 0.0);

        if (isExecution) {
            world.spawnParticles(ParticleTypes.SOUL,
                    victim.getX(), victim.getY() + victim.getHeight() * 0.5, victim.getZ(),
                    15, 0.4, 0.4, 0.4, 0.06);
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    victim.getX(), victim.getY() + 0.3, victim.getZ(),
                    8, 0.3, 0.3, 0.3, 0.03);
        }
    }

    // ========== STCS 剑封锁 ==========

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void doctor_m$stcsBlock(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (STCS_BLOCK_LOCK.get()) return;
        if (STCS_AOE_LOCK.get()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;

        if (!player.isSneaking()) return;

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

        if (source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL) ||
                source.isOf(net.minecraft.entity.damage.DamageTypes.OUT_OF_WORLD) ||
                source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        float costPerDamage = stcsItem.getEnergyCostPerDamage();
        int energyCost = (int) Math.ceil(amount * costPerDamage);
        energyCost = Math.max(CONFIG.stcsMinEnergyCost, energyCost);

        int energy = stcsItem.getEnergy(stcsStack);
        if (energy < energyCost) return;

        stcsItem.addEnergy(stcsStack, -energyCost);

        float reduction = stcsItem.isCoreActive(stcsStack) ? 1.0f : stcsItem.getBlockDamageReduction();
        reduction = Math.max(0f, Math.min(1f, reduction));

        if (reduction >= 0.8f) {
            spawnStcsBlockEffect(player);
        }

        if (reduction >= 1.0f) {
            cir.setReturnValue(false);
            return;
        }

        float newAmount = amount * (1.0f - reduction);
        if (newAmount <= 0.01f) {
            cir.setReturnValue(false);
            return;
        }

        STCS_BLOCK_LOCK.set(true);
        STCS_AOE_LOCK.set(true);
        try {
            player.damage(source, newAmount);
        } finally {
            STCS_BLOCK_LOCK.set(false);
            STCS_AOE_LOCK.set(false);
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

    // ========== STCS 范围伤害共享（AoE） ==========

    private static boolean isHoldingSTCS(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof STCSItem
                || player.getOffHandStack().getItem() instanceof STCSItem;
    }

    private static void applyStcsAoE(LivingEntity victim, LivingEntity attacker, float amount) {
        if (STCS_AOE_LOCK.get()) return;
        if (!(victim.getWorld() instanceof ServerWorld world)) return;

        STCS_AOE_LOCK.set(true);
        try {
            double radius = CONFIG.stcsAoeRadius;
            double radiusSq = radius * radius;
            Box aoeBox = new Box(victim.getPos(), victim.getPos()).expand(radius);

            for (Entity entity : world.getOtherEntities(victim, aoeBox)) {
                if (entity == attacker) continue;
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.squaredDistanceTo(victim) > radiusSq) continue;

                if (attacker instanceof ServerPlayerEntity serverAttacker) {
                    living.damage(serverAttacker.getDamageSources().playerAttack(serverAttacker), amount);
                } else {
                    living.damage(victim.getDamageSources().generic(), amount);
                }
            }
        } finally {
            STCS_AOE_LOCK.set(false);
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void doctor_m$stcsAoE(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (STCS_AOE_LOCK.get()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(source.getAttacker() instanceof PlayerEntity player)) return;
        if (!isHoldingSTCS(player)) return;

        applyStcsAoE(self, player, amount);
    }
}