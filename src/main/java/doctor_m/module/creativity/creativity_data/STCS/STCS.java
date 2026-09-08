package doctor_m.module.creativity.creativity_data.STCS;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class STCS extends Item {

    public static final String STCS_TAG = "STCS";
    public static final String ENERGY_KEY = "energy";
    public static final String MAX_ENERGY_BASE_KEY = "max_energy_base";
    public static final String CORE_ACTIVE_KEY = "core_active";
    public static final String CORE_COOLDOWN_KEY = "core_cooldown";
    public static final String CORE_MAX_COOLDOWN_KEY = "core_max_cooldown";
    public static final String SKILL_COOLDOWN_KEY = "skill_cooldown";
    public static final String MODULES_KEY = "modules";
    public static final String KIT_MODULE_KEY = "kit";
    public static final String SPECIAL_MODULE_KEY = "special";
    public static final String REGULAR_MODULES_KEY = "regular";

    public static final int BASE_ENERGY_REGEN = 4;
    public static final int CORE_ENERGY_COST = 20;
    public static final int BLOCK_ENERGY_COST = 80;
    public static final int DEFAULT_CORE_COOLDOWN_SEC = 240;

    public static final float STCH_SKILL_DAMAGE = 120f;
    public static final double STCH_SKILL_RADIUS = 6.0;
    public static final int STCH_SKILL_COST = 2000;
    public static final int STCH_SKILL_COOLDOWN = 15 * 20;

    public static final double STCA_SKILL_RADIUS = 8.0;
    public static final float STCA_SKILL_DAMAGE = 12f;
    public static final int STCA_SKILL_COST = 400;
    public static final int STCA_SKILL_COOLDOWN = 20 * 20;

    public static final double STCL_SKILL_DASH = 6.0;
    public static final int STCL_SKILL_COST = 200;
    public static final int STCL_SKILL_COOLDOWN = 1 * 20;

    private static final UUID CORE_DAMAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORE_SPEED_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    protected final String variantId;
    protected final float baseDamage;
    protected final float baseAttackSpeed;
    protected final int maxEnergy;
    protected final float blockDamageReduction;
    protected final String descriptionKey;

    public STCS(Settings settings, String variantId, float baseDamage, float baseAttackSpeed,
                    int maxEnergy, float blockDamageReduction, String descriptionKey) {
        super(settings.maxCount(1));
        this.variantId = variantId;
        this.baseDamage = baseDamage;
        this.baseAttackSpeed = baseAttackSpeed;
        this.maxEnergy = maxEnergy;
        this.blockDamageReduction = blockDamageReduction;
        this.descriptionKey = descriptionKey;
    }

    // ==================== NBT Management ====================

    public NbtCompound getOrCreateSTCSNbt(ItemStack stack) {
        NbtCompound root = stack.getOrCreateNbt();
        if (!root.contains(STCS_TAG, NbtElement.COMPOUND_TYPE)) {
            root.put(STCS_TAG, createDefaultNbt());
        }
        return root.getCompound(STCS_TAG);
    }

    private NbtCompound createDefaultNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(ENERGY_KEY, maxEnergy);
        nbt.putInt(MAX_ENERGY_BASE_KEY, maxEnergy);
        nbt.putBoolean(CORE_ACTIVE_KEY, false);
        nbt.putInt(CORE_COOLDOWN_KEY, 0);
        nbt.putInt(CORE_MAX_COOLDOWN_KEY, DEFAULT_CORE_COOLDOWN_SEC * 20);
        nbt.putInt(SKILL_COOLDOWN_KEY, 0);

        NbtCompound modules = new NbtCompound();
        modules.putString(KIT_MODULE_KEY, "");
        modules.putString(SPECIAL_MODULE_KEY, "");
        modules.put(REGULAR_MODULES_KEY, new NbtList());
        nbt.put(MODULES_KEY, modules);
        return nbt;
    }

    // ==================== Quick NBT Accessors ====================

    public int getEnergy(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getInt(ENERGY_KEY);
    }

    public void setEnergy(ItemStack stack, int value) {
        getOrCreateSTCSNbt(stack).putInt(ENERGY_KEY, Math.min(Math.max(value, 0), getMaxEnergy(stack)));
    }

    public void addEnergy(ItemStack stack, int amount) {
        setEnergy(stack, getEnergy(stack) + amount);
    }

    public int getMaxEnergy(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getInt(MAX_ENERGY_BASE_KEY);
    }

    public float getEnergyCostPerDamage() {
        return 30.0f;
    }

    public boolean isCoreActive(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getBoolean(CORE_ACTIVE_KEY);
    }

    public void setCoreActive(ItemStack stack, boolean active) {
        getOrCreateSTCSNbt(stack).putBoolean(CORE_ACTIVE_KEY, active);
    }

    public int getCoreCooldown(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getInt(CORE_COOLDOWN_KEY);
    }

    public void setCoreCooldown(ItemStack stack, int ticks) {
        getOrCreateSTCSNbt(stack).putInt(CORE_COOLDOWN_KEY, Math.max(ticks, 0));
    }

    public int getMaxCoreCooldownTicks(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getInt(CORE_MAX_COOLDOWN_KEY);
    }

    public int getSkillCooldown(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getInt(SKILL_COOLDOWN_KEY);
    }

    public void setSkillCooldown(ItemStack stack, int ticks) {
        getOrCreateSTCSNbt(stack).putInt(SKILL_COOLDOWN_KEY, Math.max(ticks, 0));
    }

    public float getBlockDamageReduction() {
        return this.blockDamageReduction;
    }

    // ==================== Attribute Modifiers ====================

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot);
        }

        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(
                        ATTACK_DAMAGE_MODIFIER_ID,
                        "tooltip.name.doctor_m.stcs.weapon_modifier",
                        baseDamage - 1.0f,
                        EntityAttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(
                        ATTACK_SPEED_MODIFIER_ID,
                        "tooltip.name.doctor_m.stcs.weapon_modifier",
                        baseAttackSpeed - 4.0f,
                        EntityAttributeModifier.Operation.ADDITION
                )
        );
        return builder.build();
    }

    // ==================== Core Attributes ====================

    public void applyCoreAttributes(ServerPlayerEntity player) {
        var damageAttr = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null && damageAttr.getModifier(CORE_DAMAGE_UUID) == null) {
            damageAttr.addPersistentModifier(new EntityAttributeModifier(
                    CORE_DAMAGE_UUID,
                    "tooltip.name.doctor_m.stcs.core_damage",
                    6.0,
                    EntityAttributeModifier.Operation.ADDITION
            ));
        }
        var speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null && speedAttr.getModifier(CORE_SPEED_UUID) == null) {
            speedAttr.addPersistentModifier(new EntityAttributeModifier(
                    CORE_SPEED_UUID,
                    "tooltip.name.doctor_m.stcs.core_speed",
                    0.2,
                    EntityAttributeModifier.Operation.MULTIPLY_BASE
            ));
        }
    }

    public void removeCoreAttributes(ServerPlayerEntity player) {
        var damageAttr = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.removeModifier(CORE_DAMAGE_UUID);
        }
        var speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(CORE_SPEED_UUID);
        }
    }

    // ==================== Skill and Core Activation ====================

    public void onSkillPressed(ServerPlayerEntity player, ItemStack stack) {
        // 由子类实现
    }

    public void onCorePressed(ServerPlayerEntity player, ItemStack stack) {
        if (getCoreCooldown(stack) > 0) return;
        if (isCoreActive(stack)) {
            setCoreActive(stack, false);
            setCoreCooldown(stack, getMaxCoreCooldownTicks(stack));
            removeCoreAttributes(player);
        } else {
            int minEnergy = CORE_ENERGY_COST * 40;
            if (getEnergy(stack) < minEnergy) {
                player.sendMessage(Text.translatable("message.doctor_m.stcs.core_low_energy")
                        .formatted(Formatting.RED), true);
                return;
            }
            setCoreActive(stack, true);
            applyCoreAttributes(player);
        }
    }

    // ==================== Inventory Tick ====================

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient()) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;

        boolean isHeld = selected || slot == 40; // 40 = offhand
        if (!isHeld) {
            if (isCoreActive(stack)) {
                setCoreActive(stack, false);
                setCoreCooldown(stack, getMaxCoreCooldownTicks(stack));
                removeCoreAttributes(player);
            }
            return;
        }

        boolean isBlocking = player.isSneaking() &&
                (player.getMainHandStack() == stack || player.getOffHandStack() == stack);

        updateActionBar(player, stack, isBlocking);
        if (isCoreActive(stack)) {
            spawnCoreParticles(player);
        }

        if (world.getTime() % 20 != 0L) return;

        int skillCd = getSkillCooldown(stack);
        if (skillCd > 0) setSkillCooldown(stack, Math.max(skillCd - 20, 0));
        int coreCd = getCoreCooldown(stack);
        if (coreCd > 0) setCoreCooldown(stack, Math.max(coreCd - 20, 0));

        if (!isCoreActive(stack) && !isBlocking) {
            addEnergy(stack, BASE_ENERGY_REGEN * 20);
        }

        if (selected && isCoreActive(stack)) {
            int cost = CORE_ENERGY_COST * 20;
            if (getEnergy(stack) < cost) {
                setCoreActive(stack, false);
                setCoreCooldown(stack, getMaxCoreCooldownTicks(stack));
                removeCoreAttributes(player);
            } else {
                addEnergy(stack, -cost);
                applyCoreAttributes(player);
            }
        } else if (selected && !isCoreActive(stack)) {
            removeCoreAttributes(player);
        }
    }

    // ==================== UI ====================

    private void updateActionBar(ServerPlayerEntity player, ItemStack stack, boolean isBlocking) {
        int energy = getEnergy(stack);
        int maxE = getMaxEnergy(stack);
        int coreCd = getCoreCooldown(stack);
        int skillCd = getSkillCooldown(stack);

        int filled = Math.min(Math.max((int)((energy / (float) maxE) * 20), 0), 20);
        StringBuilder barBuilder = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) barBuilder.append("█");
        barBuilder.append("§7");
        for (int i = 0; i < 20 - filled; i++) barBuilder.append("░");
        String bar = barBuilder.toString();

        Text coreText;
        if (isCoreActive(stack)) {
            coreText = Text.translatable("message.doctor_m.stcs.core.active");
        } else if (coreCd > 0) {
            coreText = Text.translatable("message.doctor_m.stcs.core.cooldown", coreCd / 20);
        } else {
            coreText = Text.translatable("message.doctor_m.stcs.core.ready");
        }

        Text skillText;
        if (skillCd > 0) {
            skillText = Text.translatable("message.doctor_m.stcs.skill.cooldown", skillCd / 20);
        } else {
            skillText = Text.translatable("message.doctor_m.stcs.skill.ready");
        }

        Text blockText = isBlocking ? Text.translatable("message.doctor_m.stcs.blocking") : Text.literal("");

        Text message = Text.literal("")
                .append(Text.translatable("message.doctor_m.stcs.prefix"))
                .append(Text.literal(variantId + " "))
                .append(Text.literal(bar + " "))
                .append(Text.translatable("message.doctor_m.stcs.energy_format", energy, maxE))
                .append(Text.literal("  "))
                .append(coreText)
                .append(Text.literal("  "))
                .append(skillText)
                .append(blockText);

        player.sendMessage(message, true);
    }

    private void spawnCoreParticles(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 0.1, player.getZ(),
                2, 0.3, 0.0, 0.3, 0.01
        );
        if (player.age % 5 == 0) {
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    1, 0.2, 0.2, 0.2, 0.01
            );
        }
    }

    // ==================== Tooltip ====================

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.title", variantId));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable(descriptionKey).formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.damage", (int) baseDamage));
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.attack_speed", baseAttackSpeed));
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.max_energy", maxEnergy));
        tooltip.add(Text.translatable("tooltip.doctor_m.stcs.block_reduction", (int)(blockDamageReduction * 100)));

        NbtCompound modules = getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY);
        String kit = modules.getString(KIT_MODULE_KEY);
        if (!kit.isEmpty()) tooltip.add(Text.translatable("tooltip.doctor_m.stcs.kit", kit));
        String special = modules.getString(SPECIAL_MODULE_KEY);
        if (!special.isEmpty()) tooltip.add(Text.translatable("tooltip.doctor_m.stcs.special", special));

        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.stcs." + variantId.toLowerCase().replace("-", "_") + "_detail")
        );
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
        super.appendTooltip(stack, world, tooltip, context);
    }

    // ==================== Module Interface ====================

    public String getKitModule(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).getString(KIT_MODULE_KEY);
    }

    public void setKitModule(ItemStack stack, String id) {
        getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).putString(KIT_MODULE_KEY, id);
    }

    public String getSpecialModule(ItemStack stack) {
        return getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).getString(SPECIAL_MODULE_KEY);
    }

    public void setSpecialModule(ItemStack stack, String id) {
        getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY).putString(SPECIAL_MODULE_KEY, id);
    }

    public List<String> getRegularModules(ItemStack stack) {
        NbtList list = getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY)
                .getList(REGULAR_MODULES_KEY, NbtElement.STRING_TYPE);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    public boolean addRegularModule(ItemStack stack, String id) {
        NbtCompound modules = getOrCreateSTCSNbt(stack).getCompound(MODULES_KEY);
        NbtList list = modules.getList(REGULAR_MODULES_KEY, NbtElement.STRING_TYPE);
        if (list.size() >= 4) return false;
        NbtList newList = new NbtList();
        for (int i = 0; i < list.size(); i++) {
            newList.add(NbtString.of(list.getString(i)));
        }
        newList.add(NbtString.of(id));
        modules.put(REGULAR_MODULES_KEY, newList);
        return true;
    }
}