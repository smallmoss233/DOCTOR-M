package doctor_m.Item.stcs

import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import net.minecraft.util.hit.HitResult
import net.minecraft.world.RaycastContext

/** STC-07H "巨妖" — 重装型 */
class STCHItem : STCSItem(
    settings = Item.Settings().rarity(Rarity.EPIC),
    variantId = "STC-07H",
    baseDamage = 30f,
    baseAttackSpeed = 1.2f,
    maxEnergy = 10000,
    blockDamageReduction = 0.90f,
    descriptionKey = "message.doctor_m.stch.description"
) {
    override fun onSkillPressed(player: ServerPlayerEntity, stack: ItemStack) {
        val cd = getSkillCooldown(stack)
        if (cd > 0) return
        if (getEnergy(stack) < STCH_SKILL_COST) {
            player.sendMessage(
                Text.translatable("message.doctor_m.stcs.skill_low_energy")
                    .formatted(Formatting.RED), true
            )
            return
        }

        setEnergy(stack, getEnergy(stack) - STCH_SKILL_COST)

        val box = player.boundingBox.expand(STCH_SKILL_RADIUS)
        player.serverWorld.getEntitiesByClass(LivingEntity::class.java, box) { it != player }.forEach {
            it.hurtTime = 0
            it.timeUntilRegen = 0
            it.damage(player.damageSources.playerAttack(player), STCH_SKILL_DAMAGE)
        }

        player.world.playSound(null, player.x, player.y, player.z,
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f)

        var finalCd = STCH_SKILL_COOLDOWN
        if (isCoreActive(stack)) finalCd -= 40
        setSkillCooldown(stack, finalCd)
    }
}

/** STC-08A "天图" — 标准型 */
class STCAItem : STCSItem(
    settings = Item.Settings().rarity(Rarity.EPIC),
    variantId = "STC-08A",
    baseDamage = 24f,
    baseAttackSpeed = 2.0f,
    maxEnergy = 10000,
    blockDamageReduction = 0.85f,
    descriptionKey = "message.doctor_m.stca.description"

) {
    override fun onSkillPressed(player: ServerPlayerEntity, stack: ItemStack) {
        val cd = getSkillCooldown(stack)
        if (cd > 0) return
        if (getEnergy(stack) < STCA_SKILL_COST) {
            player.sendMessage(
                Text.translatable("message.doctor_m.stcs.skill_low_energy")
                    .formatted(Formatting.RED), true
            )
            return
        }

        setEnergy(stack, getEnergy(stack) - STCA_SKILL_COST)

        val box = player.boundingBox.expand(STCA_SKILL_RADIUS)
        player.serverWorld.getEntitiesByClass(LivingEntity::class.java, box) { it != player }.forEach {
            it.damage(player.damageSources.playerAttack(player), STCA_SKILL_DAMAGE)
            val dir = it.pos.subtract(player.pos).normalize()
            it.addVelocity(dir.x * 2.0, 0.5, dir.z * 2.0)
            it.velocityDirty = true

            if (it is ServerPlayerEntity) {
                it.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(it))
            }
        }

        player.world.playSound(null, player.x, player.y, player.z,
            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.5f)

        var finalCd = STCA_SKILL_COOLDOWN
        if (isCoreActive(stack)) finalCd -= 40
        setSkillCooldown(stack, finalCd)
    }
}

/** STC-09L "游侠" — 突袭型 */
class STCLItem : STCSItem(
    settings = Item.Settings().rarity(Rarity.EPIC),
    variantId = "STC-09L",
    baseDamage = 20f,
    baseAttackSpeed = 2.8f,
    maxEnergy = 10000,
    blockDamageReduction = 0.80f,
    descriptionKey = "message.doctor_m.stcl.description"
) {
    override fun onSkillPressed(player: ServerPlayerEntity, stack: ItemStack) {
        val cd = getSkillCooldown(stack)
        if (cd > 0) return
        if (getEnergy(stack) < STCL_SKILL_COST) {
            player.sendMessage(
                Text.translatable("message.doctor_m.stcs.skill_low_energy")
                    .formatted(Formatting.RED), true
            )
            return
        }

        setEnergy(stack, getEnergy(stack) - STCL_SKILL_COST)

        val eyePos = player.pos.add(0.0, player.standingEyeHeight.toDouble(), 0.0)
        val look = player.rotationVector
        val end = eyePos.add(look.multiply(STCL_SKILL_DASH))

        val result = player.world.raycast(
            RaycastContext(
                eyePos, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        )

        val targetPos = if (result.type == HitResult.Type.BLOCK) {
            result.pos.subtract(look.multiply(0.5))
        } else {
            end
        }

        player.requestTeleport(targetPos.x, targetPos.y, targetPos.z)
        player.fallDistance = 0f

        player.world.playSound(null, player.x, player.y, player.z,
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f)

        var finalCd = STCL_SKILL_COOLDOWN
        if (isCoreActive(stack)) finalCd -= 20
        finalCd = finalCd.coerceAtLeast(0)
        setSkillCooldown(stack, finalCd)
    }
}