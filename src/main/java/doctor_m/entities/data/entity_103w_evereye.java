package doctor_m.entities.data;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class entity_103w_evereye extends PathAwareEntity {

    private long lastCounterAttackTime = 20;   // 反击冷却（防止连续触发）
    private boolean isAngry = false;          // 激怒标志（针对玩家）
    private int angerTimer = 40;               // 激怒持续时间（tick）

    public entity_103w_evereye(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();

        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
        this.goalSelector.add(4, new WanderAroundGoal(this, 0.6, 20));
    }

    @Override
    public void tick() {
        super.tick();

        // 激怒计时器递减
        if (isAngry && !this.getWorld().isClient) {
            if (angerTimer > 40) {
                angerTimer--;
                // 在激怒持续期间（例如 20 tick）内，如果还保留攻击目标，可以尝试攻击，但只需要一次
                // 我们可以在攻击后清除激怒，所以这里只做计时，不清除目标
            } else {
                isAngry = false;
                // 清除攻击目标（如果有）
                this.setAttacker(null);
                this.setTarget(null);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (!damaged) return false;

        if (!this.getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker) {
            long now = this.getWorld().getTime();
            if (now - lastCounterAttackTime < 30) return true; // 冷却 1.5 秒
            this.lastCounterAttackTime = now;

            // 区分玩家和怪物
            if (attacker instanceof PlayerEntity) {
                // 玩家攻击：进入激怒状态，并尝试反击一次
                if (!isAngry) {
                    isAngry = true;
                    angerTimer = 20; // 激怒持续 1 秒（20 tick），足够发动一次攻击
                    // 设置攻击目标为玩家
                    this.setTarget(attacker);
                    // 添加攻击目标选择器（临时），或者通过直接调用攻击
                    // 由于攻击执行会在下一个 tick，我们这里直接触发攻击逻辑
                    this.tryAttack(attacker); // 立即攻击一次
                    // 攻击后清除目标（让 angerTimer 自行归零）
                }
                // 攻击后，激怒标志保持到计时结束
            } else if (attacker instanceof HostileEntity) {
                // 怪物攻击：根据血量决定
                double healthPercentage = this.getHealth() / this.getMaxHealth();
                if (healthPercentage < 0.5) {
                    applyParadoxDamage(attacker);
                } else {
                    applyInvisibility(attacker);
                }
            }
        }
        return true;
    }

    /** 隐形并加速 */
    private void applyInvisibility(LivingEntity attacker) {
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, false, false));
    }

    /** 悖论伤害：对攻击者造成其最大生命值 100% 的伤害 */
    private void applyParadoxDamage(LivingEntity attacker) {
        float damage = attacker.getMaxHealth();
        attacker.damage(this.getDamageSources().magic(), damage);
        this.getWorld().sendEntityStatus(this, (byte) 4);
        if (attacker instanceof PlayerEntity player) {
            player.sendMessage(net.minecraft.text.Text.literal("§c你遭到了悖论打击！"), true);
        }
    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
    }
}