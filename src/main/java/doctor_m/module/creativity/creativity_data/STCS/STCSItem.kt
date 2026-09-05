package doctor_m.Item.stcs

import doctor_m.module.EmissiveItem
import doctor_m.network.INVERTSCREENPACKETNetwork
import doctor_m.util.creativity.DynamicColorHelper
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import net.minecraft.util.hit.HitResult
import net.minecraft.world.RaycastContext
import org.spongepowered.asm.mixin.injection.selectors.ElementNode.listOf
import java.awt.Color
import java.lang.Math.cos
import java.lang.Math.sin

/** STC-07H "巨妖" — 重装型 */
class STCHItem : STCSItem(
    settings = Item.Settings().rarity(Rarity.EPIC),
    variantId = "STC-07H",
    baseDamage = 30f,
    baseAttackSpeed = 1.2f,
    maxEnergy = 10000,
    blockDamageReduction = 0.90f,
    descriptionKey = "message.doctor_m.stch.description"
), EmissiveItem {

    override fun getEnergyCostPerDamage(): Float = 10.0f

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
        player.serverWorld.getEntitiesByClass(LivingEntity::class.java, box) { it != player }.forEach { entity ->
            entity.hurtTime = 0
            entity.timeUntilRegen = 0
            entity.damage(player.damageSources.playerAttack(player), STCH_SKILL_DAMAGE)

            // 受击实体粒子：横扫 + 暴击
            entity.world.addParticle(
                ParticleTypes.SWEEP_ATTACK,
                entity.x, entity.y + entity.height / 2, entity.z,
                0.0, 0.0, 0.0
            )
            entity.world.addParticle(
                ParticleTypes.CRIT,
                entity.x, entity.y + entity.height, entity.z,
                0.0, 0.5, 0.0
            )
        }

        // 玩家周围环形粒子
        for (i in 0 until 36) {
            val angle = Math.toRadians((i * 10).toDouble())
            val x = player.x + cos(angle) * STCH_SKILL_RADIUS
            val z = player.z + sin(angle) * STCH_SKILL_RADIUS
            player.serverWorld.spawnParticles(
                ParticleTypes.SWEEP_ATTACK,
                x, player.y + 0.5, z,
                1, 0.0, 0.0, 0.0, 0.1
            )
        }

        player.world.playSound(null, player.x, player.y, player.z,
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f)

        var finalCd = STCH_SKILL_COOLDOWN
        if (isCoreActive(stack)) finalCd -= 40
        setSkillCooldown(stack, finalCd)
    }

    override fun getName(stack: ItemStack): Text {
        val baseName = super.getName(stack)
        val colors = listOf(
            Color(128, 0, 128),
            Color(128, 0, 128),
            Color(255, 165, 0)
        )
        return DynamicColorHelper.applyColorCycle(baseName, colors, 15000)
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
), EmissiveItem {

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
        player.serverWorld.getEntitiesByClass(LivingEntity::class.java, box) { it != player }.forEach { entity ->
            entity.damage(player.damageSources.playerAttack(player), STCA_SKILL_DAMAGE)
            val dir = entity.pos.subtract(player.pos).normalize()
            entity.addVelocity(dir.x * 2.0, 0.5, dir.z * 2.0)
            entity.velocityDirty = true

            if (entity is ServerPlayerEntity) {
                entity.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(entity))
            }

            // 弹开粒子：白色烟雾
            entity.world.addParticle(
                ParticleTypes.POOF,
                entity.x, entity.y + 1.0, entity.z,
                0.0, 0.2, 0.0
            )
        }

        // 玩家位置爆炸烟雾
        player.serverWorld.spawnParticles(
            ParticleTypes.EXPLOSION,
            player.x, player.y + 1.0, player.z,
            3, 0.5, 0.5, 0.5, 1.0
        )
        player.serverWorld.spawnParticles(
            ParticleTypes.LARGE_SMOKE,
            player.x, player.y + 1.0, player.z,
            10, 0.8, 0.5, 0.8, 0.05
        )

        player.world.playSound(null, player.x, player.y, player.z,
            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.5f)

        var finalCd = STCA_SKILL_COOLDOWN
        if (isCoreActive(stack)) finalCd -= 40
        setSkillCooldown(stack, finalCd)
    }

    override fun getName(stack: ItemStack): Text {
        val baseName = super.getName(stack)
        val colors = listOf(
            Color(128, 0, 128),
            Color(128, 0, 128),
            Color(0, 100, 255)
        )
        return DynamicColorHelper.applyColorCycle(baseName, colors, 15000)
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
), EmissiveItem {

    override fun getEnergyCostPerDamage(): Float = 50.0f

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

        // ===== 起点粒子爆发 =====
        spawnBurstParticles(player, player.x, player.y + 1.0, player.z)

        // ===== 路径拖尾粒子 =====
        val start = player.pos.add(0.0, player.standingEyeHeight.toDouble() - 0.5, 0.0)
        val endVec = targetPos.add(0.0, 0.5, 0.0)
        val distance = start.distanceTo(endVec)
        val steps = (distance * 4).toInt().coerceAtLeast(1) // 增加拖尾密度

        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val x = start.x + (endVec.x - start.x) * t
            val y = start.y + (endVec.y - start.y) * t
            val z = start.z + (endVec.z - start.z) * t
            // 使用多种粒子混合，形成更明显的拖尾
            if (i % 2 == 0) {
                player.world.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, 0.0, 0.0)
            } else {
                player.world.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.0, 0.0)
            }
            // 随机扩散粒子
            if (i % 3 == 0) {
                player.world.addParticle(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2
                )
            }
        }

        // ===== 终点粒子爆发 =====
        spawnBurstParticles(player, targetPos.x, targetPos.y + 1.0, targetPos.z)

        // ===== 传送执行 =====
        player.requestTeleport(targetPos.x, targetPos.y, targetPos.z)
        player.fallDistance = 0f

        // ===== 发送反色视角效果数据包 =====
        INVERTSCREENPACKETNetwork.sendInvertScreenPacket(player, 10) // 20 ticks = 1秒

        player.world.playSound(null, player.x, player.y, player.z,
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f)

        var finalCd = STCL_SKILL_COOLDOWN
        if (isCoreActive(stack)) finalCd -= 20
        finalCd = finalCd.coerceAtLeast(0)
        setSkillCooldown(stack, finalCd)
    }

    private fun spawnBurstParticles(player: ServerPlayerEntity, x: Double, y: Double, z: Double) {
        // 一圈旋转粒子
        for (i in 0 until 20) {
            val angle = Math.toRadians((i * 18).toDouble())
            val radius = 0.8
            val px = x + Math.cos(angle) * radius
            val pz = z + Math.sin(angle) * radius
            player.serverWorld.spawnParticles(ParticleTypes.END_ROD, px, y, pz, 1, 0.0, 0.1, 0.0, 0.0)
        }
        // 中央爆炸粒子
        player.serverWorld.spawnParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 1.0)
        // 烟雾
        player.serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 8, 0.3, 0.2, 0.3, 0.05)
    }

    override fun getName(stack: ItemStack): Text {
        val baseName = super.getName(stack)
        val colors = listOf(
            Color(128, 0, 128),
            Color(128, 0, 128),
            Color(255, 0, 0)
        )
        return DynamicColorHelper.applyColorCycle(baseName, colors, 15000)
    }
}