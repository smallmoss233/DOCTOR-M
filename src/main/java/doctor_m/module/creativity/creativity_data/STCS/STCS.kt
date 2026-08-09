package doctor_m.Item.stcs

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import doctor_m.util.tooltip.ShiftTooltipInvoker
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import java.util.*

abstract class STCSItem(
    settings: Settings,
    val variantId: String,
    val baseDamage: Float,
    val baseAttackSpeed: Float,
    val maxEnergy: Int,
    val blockDamageReduction: Float,
    val descriptionKey: String
) : Item(settings.maxCount(1)) {

    companion object {
        const val STCS_TAG = "STCS"
        const val ENERGY_KEY = "energy"
        const val MAX_ENERGY_BASE_KEY = "max_energy_base"
        const val CORE_ACTIVE_KEY = "core_active"
        const val CORE_COOLDOWN_KEY = "core_cooldown"
        const val CORE_MAX_COOLDOWN_KEY = "core_max_cooldown"
        const val SKILL_COOLDOWN_KEY = "skill_cooldown"
        const val MODULES_KEY = "modules"
        const val KIT_MODULE_KEY = "kit"
        const val SPECIAL_MODULE_KEY = "special"
        const val REGULAR_MODULES_KEY = "regular"

        const val BASE_ENERGY_REGEN = 4
        const val CORE_ENERGY_COST = 20
        const val BLOCK_ENERGY_COST = 80
        const val DEFAULT_CORE_COOLDOWN_SEC = 240

        const val STCH_SKILL_DAMAGE = 120f
        const val STCH_SKILL_RADIUS = 6.0
        const val STCH_SKILL_COST = 2000
        const val STCH_SKILL_COOLDOWN = 15 * 20

        const val STCA_SKILL_RADIUS = 8.0
        const val STCA_SKILL_DAMAGE = 12f
        const val STCA_SKILL_COST = 400
        const val STCA_SKILL_COOLDOWN = 20 * 20

        const val STCL_SKILL_DASH = 6.0
        const val STCL_SKILL_COST = 200
        const val STCL_SKILL_COOLDOWN = 1 * 20

        private val CORE_DAMAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val CORE_SPEED_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    }

    // ========== NBT ==========

    fun getOrCreateSTCSNbt(stack: ItemStack): NbtCompound {
        val root = stack.orCreateNbt
        if (!root.contains(STCS_TAG, NbtElement.COMPOUND_TYPE.toInt())) {
            root.put(STCS_TAG, createDefaultNbt())
        }
        return root.getCompound(STCS_TAG)
    }

    private fun createDefaultNbt(): NbtCompound {
        return NbtCompound().apply {
            putInt(ENERGY_KEY, maxEnergy)
            putInt(MAX_ENERGY_BASE_KEY, maxEnergy)
            putBoolean(CORE_ACTIVE_KEY, false)
            putInt(CORE_COOLDOWN_KEY, 0)
            putInt(CORE_MAX_COOLDOWN_KEY, DEFAULT_CORE_COOLDOWN_SEC * 20)
            putInt(SKILL_COOLDOWN_KEY, 0)
            put(MODULES_KEY, NbtCompound().apply {
                putString(KIT_MODULE_KEY, "")
                putString(SPECIAL_MODULE_KEY, "")
                put(REGULAR_MODULES_KEY, NbtList())
            })
        }
    }

    // ========== 快捷读写 ==========

    fun getEnergy(stack: ItemStack): Int = getOrCreateSTCSNbt(stack).getInt(ENERGY_KEY)
    fun setEnergy(stack: ItemStack, value: Int) {
        getOrCreateSTCSNbt(stack).putInt(ENERGY_KEY, value.coerceIn(0, getMaxEnergy(stack)))
    }
    fun addEnergy(stack: ItemStack, amount: Int) = setEnergy(stack, getEnergy(stack) + amount)

    open fun getMaxEnergy(stack: ItemStack): Int {
        return getOrCreateSTCSNbt(stack).getInt(MAX_ENERGY_BASE_KEY)
    }

    fun isCoreActive(stack: ItemStack): Boolean = getOrCreateSTCSNbt(stack).getBoolean(CORE_ACTIVE_KEY)
    fun setCoreActive(stack: ItemStack, active: Boolean) {
        getOrCreateSTCSNbt(stack).putBoolean(CORE_ACTIVE_KEY, active)
    }

    fun getCoreCooldown(stack: ItemStack): Int = getOrCreateSTCSNbt(stack).getInt(CORE_COOLDOWN_KEY)
    fun setCoreCooldown(stack: ItemStack, ticks: Int) {
        getOrCreateSTCSNbt(stack).putInt(CORE_COOLDOWN_KEY, ticks.coerceAtLeast(0))
    }

    open fun getMaxCoreCooldownTicks(stack: ItemStack): Int {
        return getOrCreateSTCSNbt(stack).getInt(CORE_MAX_COOLDOWN_KEY)
    }

    fun getSkillCooldown(stack: ItemStack): Int = getOrCreateSTCSNbt(stack).getInt(SKILL_COOLDOWN_KEY)
    fun setSkillCooldown(stack: ItemStack, ticks: Int) {
        getOrCreateSTCSNbt(stack).putInt(SKILL_COOLDOWN_KEY, ticks.coerceAtLeast(0))
    }

    // ========== 攻击属性 ==========

    override fun getAttributeModifiers(slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot)
        val builder = ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
        builder.put(
            EntityAttributes.GENERIC_ATTACK_DAMAGE,
            EntityAttributeModifier(
                ATTACK_DAMAGE_MODIFIER_ID, "tooltip.name.doctor_m.stcs.weapon_modifier",
                (baseDamage - 1.0f).toDouble(), EntityAttributeModifier.Operation.ADDITION
            )
        )
        builder.put(
            net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_SPEED,
            EntityAttributeModifier(
                ATTACK_SPEED_MODIFIER_ID, "tooltip.name.doctor_m.stcs.weapon_modifier",
                (baseAttackSpeed - 4.0f).toDouble(), EntityAttributeModifier.Operation.ADDITION
            )
        )
        return builder.build()
    }

    // ========== 核心属性（带重复添加保护） ==========

    open fun applyCoreAttributes(player: ServerPlayerEntity) {
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)?.apply {
            if (getModifier(CORE_DAMAGE_UUID) == null) {
                addPersistentModifier(EntityAttributeModifier(
                    CORE_DAMAGE_UUID, "tooltip.name.doctor_m.stcs.core_damage", 6.0, EntityAttributeModifier.Operation.ADDITION
                ))
            }
        }
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)?.apply {
            if (getModifier(CORE_SPEED_UUID) == null) {
                addPersistentModifier(EntityAttributeModifier(
                    CORE_SPEED_UUID, "tooltip.name.doctor_m.stcs.core_speed", 0.2, EntityAttributeModifier.Operation.MULTIPLY_BASE
                ))
            }
        }
    }

    open fun removeCoreAttributes(player: ServerPlayerEntity) {
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)?.removeModifier(CORE_DAMAGE_UUID)
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)?.removeModifier(CORE_SPEED_UUID)
    }

    // ========== 技能接口 ==========

    open fun onSkillPressed(player: ServerPlayerEntity, stack: ItemStack) {}

    open fun onCorePressed(player: ServerPlayerEntity, stack: ItemStack) {
        if (getCoreCooldown(stack) > 0) return
        if (isCoreActive(stack)) {
            setCoreActive(stack, false)
            setCoreCooldown(stack, getMaxCoreCooldownTicks(stack))
            removeCoreAttributes(player)
        } else {
            val minEnergy = CORE_ENERGY_COST * 40
            if (getEnergy(stack) < minEnergy) {
                player.sendMessage(
                    Text.translatable("message.doctor_m.stcs.core_low_energy")
                        .formatted(Formatting.RED), true
                )
                return
            }
            setCoreActive(stack, true)
            applyCoreAttributes(player)
        }
    }

    // ========== Tick ==========

    override fun inventoryTick(stack: ItemStack, world: World, entity: Entity, slot: Int, selected: Boolean) {
        if (world.isClient) return
        if (entity !is ServerPlayerEntity) return
        val isHeld = selected || slot == 40
        if (!isHeld) {
            if (isCoreActive(stack)) {
                setCoreActive(stack, false)
                setCoreCooldown(stack, getMaxCoreCooldownTicks(stack))
                removeCoreAttributes(entity)
            }
            return
        }

        // 每 tick：格挡检测（与 Mixin 一致：潜行 + 持有该武器）
        val isBlocking = entity.isSneaking && (entity.mainHandStack == stack || entity.offHandStack == stack)

        // 每 tick：Action Bar + 核心粒子（不写 NBT）
        updateActionBar(entity, stack, isBlocking)
        if (isCoreActive(stack)) spawnCoreParticles(entity)
        // 删除这行：if (isBlocking) spawnBlockParticles(entity)

        // 每 20 ticks：能量/冷却逻辑 + 唯一一次 NBT 写入
        if (world.time % 20 != 0L) return

        val skillCd = getSkillCooldown(stack)
        if (skillCd > 0) setSkillCooldown(stack, (skillCd - 20).coerceAtLeast(0))
        val coreCd = getCoreCooldown(stack)
        if (coreCd > 0) setCoreCooldown(stack, (coreCd - 20).coerceAtLeast(0))

        if (!isCoreActive(stack) && !isBlocking) {
            addEnergy(stack, BASE_ENERGY_REGEN * 20)
        }

        if (selected && isCoreActive(stack)) {
            val cost = CORE_ENERGY_COST * 20
            if (getEnergy(stack) < cost) {
                setCoreActive(stack, false)
                setCoreCooldown(stack, getMaxCoreCooldownTicks(stack))
                removeCoreAttributes(entity)
            } else {
                addEnergy(stack, -cost)
                applyCoreAttributes(entity)
            }
        } else if (selected && !isCoreActive(stack)) {
            removeCoreAttributes(entity)
        }
    }

    // ========== 视觉反馈 ==========

    private fun updateActionBar(player: ServerPlayerEntity, stack: ItemStack, isBlocking: Boolean) {
        val energy = getEnergy(stack)
        val maxE = getMaxEnergy(stack)
        val coreCd = getCoreCooldown(stack)
        val skillCd = getSkillCooldown(stack)

        val filled = (energy.toFloat() / maxE * 20).toInt().coerceIn(0, 20)
        val bar = "§a" + "█".repeat(filled) + "§7" + "░".repeat(20 - filled)

        val coreText = when {
            isCoreActive(stack) -> Text.translatable("message.doctor_m.stcs.core.active")
            coreCd > 0 -> Text.translatable("message.doctor_m.stcs.core.cooldown", coreCd / 20)
            else -> Text.translatable("message.doctor_m.stcs.core.ready")
        }

        val skillText = when {
            skillCd > 0 -> Text.translatable("message.doctor_m.stcs.skill.cooldown", skillCd / 20)
            else -> Text.translatable("message.doctor_m.stcs.skill.ready")
        }

        val blockText = if (isBlocking) Text.translatable("message.doctor_m.stcs.blocking") else Text.literal("")

        val message = Text.literal("")
            .append(Text.translatable("message.doctor_m.stcs.prefix"))
            .append(Text.literal("$variantId "))
            .append(Text.literal("$bar "))
            .append(Text.translatable("message.doctor_m.stcs.energy_format", energy, maxE))
            .append(Text.literal("  "))
            .append(coreText)
            .append(Text.literal("  "))
            .append(skillText)
            .append(blockText)

        player.sendMessage(message, true)
    }

    private fun spawnCoreParticles(player: ServerPlayerEntity) {
        val world = player.serverWorld
        world.spawnParticles(
            ParticleTypes.SOUL_FIRE_FLAME,
            player.x, player.y + 0.1, player.z,
            2, 0.3, 0.0, 0.3, 0.01
        )
        if (player.age % 5 == 0) {
            world.spawnParticles(
                ParticleTypes.END_ROD,
                player.x, player.y + 1.5, player.z,
                1, 0.2, 0.2, 0.2, 0.01
            )
        }
    }

    // ========== Tooltip ==========

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: net.minecraft.client.item.TooltipContext
    ) {
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.title", variantId))
        tooltip.add(Text.literal(""))
        tooltip.add(Text.translatable(descriptionKey).formatted(Formatting.GRAY))
        tooltip.add(Text.literal(""))
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.damage", baseDamage.toInt()))
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.attack_speed", baseAttackSpeed))
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.max_energy", maxEnergy))
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.block_reduction", (blockDamageReduction * 100).toInt()))

        val modules = getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY)
        val kit = modules.getString(KIT_MODULE_KEY)
        if (kit.isNotEmpty()) tooltip.add(Text.translatable("tooltip.doctor_m.stcs.kit", kit))
        val special = modules.getString(SPECIAL_MODULE_KEY)
        if (special.isNotEmpty()) tooltip.add(Text.translatable("tooltip.doctor_m.stcs.special", special))

        ShiftTooltipInvoker.addShiftTooltip(tooltip,
            Text.translatable("message.doctor_m.stcs." + variantId.lowercase().replace("-", "_") + "_detail")
        )
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"))
        super.appendTooltip(stack, world, tooltip, context)
    }

    // ========== 模块系统接口（预留） ==========

    fun getKitModule(stack: ItemStack): String =
        getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).getString(KIT_MODULE_KEY)
    fun setKitModule(stack: ItemStack, id: String) {
        getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).putString(KIT_MODULE_KEY, id)
    }
    fun getSpecialModule(stack: ItemStack): String =
        getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).getString(SPECIAL_MODULE_KEY)
    fun setSpecialModule(stack: ItemStack, id: String) {
        getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).putString(SPECIAL_MODULE_KEY, id)
    }
    fun getRegularModules(stack: ItemStack): List<String> {
        val list = getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY)
            .getList(REGULAR_MODULES_KEY, NbtElement.STRING_TYPE.toInt())
        return (0 until list.size).map { list.getString(it) }
    }
    fun addRegularModule(stack: ItemStack, id: String): Boolean {
        val modules = getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY)
        val list = modules.getList(REGULAR_MODULES_KEY, NbtElement.STRING_TYPE.toInt())
        if (list.size >= 4) return false
        val newList = NbtList()
        (0 until list.size).forEach { newList.add(NbtString.of(list.getString(it))) }
        newList.add(NbtString.of(id))
        modules.put(REGULAR_MODULES_KEY, newList)
        return true
    }
}