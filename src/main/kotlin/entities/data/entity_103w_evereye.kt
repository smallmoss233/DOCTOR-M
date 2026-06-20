package entities.data

import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.*
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.world.World

class entity_103w_evereye(entityType: EntityType<out PathAwareEntity>, world: World) : PathAwareEntity(entityType, world) {

    private var lastCounterAttackTime = 0L
    private var isAngry = false
    private var angerTimer = 0

    override fun initGoals() {
        super.initGoals()
        goalSelector.add(1, SwimGoal(this))
        goalSelector.add(2, LookAtEntityGoal(this, PlayerEntity::class.java, 8.0f))
        goalSelector.add(3, LookAroundGoal(this))
        goalSelector.add(4, WanderAroundGoal(this, 0.6, 20))
    }

    override fun tick() {
        super.tick()
        if (!world.isClient && isAngry) {
            if (angerTimer > 0) {
                angerTimer--
            } else {
                isAngry = false
                setAttacker(null)
                setTarget(null)
            }
        }
    }

    override fun damage(source: DamageSource, amount: Float): Boolean {
        val damaged = super.damage(source, amount)
        if (!damaged) return false

        if (!world.isClient && source.attacker is LivingEntity) {
            val now = world.time
            if (now - lastCounterAttackTime < 30) return true
            lastCounterAttackTime = now

            when (val attacker = source.attacker as LivingEntity) {
                is PlayerEntity -> {
                    if (!isAngry) {
                        isAngry = true
                        angerTimer = 20
                        setTarget(attacker)
                        tryAttack(attacker)
                    }
                }
                is HostileEntity -> {
                    if (health / maxHealth < 0.5) {
                        applyParadoxDamage(attacker)
                    } else {
                        applyInvisibility(attacker)
                    }
                }
            }
        }
        return true
    }

    private fun applyInvisibility(attacker: LivingEntity) {
        addStatusEffect(StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false))
        addStatusEffect(StatusEffectInstance(StatusEffects.SPEED, 200, 1, false, false))
    }

    private fun applyParadoxDamage(attacker: LivingEntity) {
        val damage = attacker.maxHealth
        attacker.damage(damageSources.magic(), damage)
        world.sendEntityStatus(this, 4)
        (attacker as? PlayerEntity)?.sendMessage(Text.literal("§c你遭到了悖论打击！"), true)
    }

    companion object {
        fun createMobAttributes(): DefaultAttributeContainer.Builder =
            PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0)
    }
}