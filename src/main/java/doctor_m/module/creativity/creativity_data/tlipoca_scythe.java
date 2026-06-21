package doctor_m.module.creativity.creativity_data;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class tlipoca_scythe extends SwordItem {
    private static final String KILL_COUNT_KEY = "TlipocaKillCount";
    private static final String GROWTH_KEY = "TlipocaGrowth";
    private static final String INIT_KEY = "TlipocaInit";
    public static final long COOLDOWN_TICKS = 60; // 3秒

    private static final ConcurrentHashMap<UUID, Long> lastSlashTime = new ConcurrentHashMap<>();

    private static final UUID DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789014");
    private static final UUID SPEED_UUID = UUID.fromString("12345678-1234-1234-1234-123456789016");

    public tlipoca_scythe(Settings settings) {
        super(TlipocaMaterial.INSTANCE, 0, -3.2f, settings);
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return ImmutableMultimap.of();
        }
        return super.getAttributeModifiers(slot);
    }

    public static void initScytheNbt(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        boolean changed = false;

        if (EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) == 0) {
            stack.addEnchantment(Enchantments.SHARPNESS, 10);
            changed = true;
        }
        if (EnchantmentHelper.getLevel(Enchantments.LOOTING, stack) == 0) {
            stack.addEnchantment(Enchantments.LOOTING, 10);
            changed = true;
        }
        if (!nbt.getBoolean("Unbreakable")) {
            nbt.putBoolean("Unbreakable", true);
            changed = true;
        }

        if (!nbt.contains("AttributeModifiers", 9)) {
            writeAttributeModifiers(stack, 20.0f);
            changed = true;
        }

        if (changed) {
            System.out.println("[TlipocaScythe] NBT initialized for stack");
        }
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        initScytheNbt(stack);
        return stack;
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        super.onCraft(stack, world, player);
        initScytheNbt(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.getBoolean(INIT_KEY)) {
            nbt.putBoolean(INIT_KEY, true);
            initScytheNbt(stack);
        }
    }

    private static void writeAttributeModifiers(ItemStack stack, float damage) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList list = new NbtList();

        NbtCompound dmg = new NbtCompound();
        dmg.putString("AttributeName", "minecraft:generic.attack_damage");
        dmg.putString("Name", "tlipoca_damage");
        dmg.putDouble("Amount", damage);
        dmg.putInt("Operation", 0);
        dmg.putUuid("UUID", DAMAGE_UUID);
        dmg.putString("Slot", "mainhand");
        list.add(dmg);

        NbtCompound spd = new NbtCompound();
        spd.putString("AttributeName", "minecraft:generic.attack_speed");
        spd.putString("Name", "tlipoca_speed");
        spd.putDouble("Amount", -3.6);
        spd.putInt("Operation", 0);
        spd.putUuid("UUID", SPEED_UUID);
        spd.putString("Slot", "mainhand");
        list.add(spd);

        nbt.put("AttributeModifiers", list);
        System.out.println("[TlipocaScythe] AttributeModifiers updated: damage=" + damage);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            if (isOnCooldown(world, user)) {
                user.sendMessage(Text.literal("§c镰刀还在冷却中！"), true);
                return TypedActionResult.fail(stack);
            }
            if (user instanceof ServerPlayerEntity serverPlayer) {
                performSlashAttack(world, serverPlayer, stack);
                setCooldown(world, user, this);
            }
        } else {
            spawnSlashParticles(user);
        }

        return TypedActionResult.success(stack);
    }

    public static boolean isOnCooldown(World world, PlayerEntity player) {
        Long last = lastSlashTime.get(player.getUuid());
        return last != null && world.getTime() - last < COOLDOWN_TICKS;
    }

    public static void setCooldown(World world, PlayerEntity player, tlipoca_scythe item) {
        lastSlashTime.put(player.getUuid(), world.getTime());
        player.getItemCooldownManager().set(item, (int) COOLDOWN_TICKS);
    }

    public static float getTotalAttackDamage(ItemStack stack) {
        int growth = stack.getOrCreateNbt().getInt(GROWTH_KEY);
        return 20.0f + growth;
    }

    public static void onKill(ItemStack stack, ServerPlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int killCount = nbt.getInt(KILL_COUNT_KEY) + 1;
        nbt.putInt(KILL_COUNT_KEY, killCount);
        System.out.println("[TlipocaScythe] Kill! Count=" + killCount);

        if (killCount % 10 == 0) {
            int growth = nbt.getInt(GROWTH_KEY) + 5;
            nbt.putInt(GROWTH_KEY, growth);
            float newDamage = 20.0f + growth;
            writeAttributeModifiers(stack, newDamage);
            player.getInventory().markDirty();
            System.out.println("[TlipocaScythe] GROWTH UP! New damage=" + newDamage);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int growth = stack.getOrCreateNbt().getInt(GROWTH_KEY);
        int kills = stack.getOrCreateNbt().getInt(KILL_COUNT_KEY);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("当前伤害: " + (20 + growth)).formatted(Formatting.DARK_RED));
        if (growth > 0) {
            tooltip.add(Text.literal("成长加成: +" + growth).formatted(Formatting.GREEN));
        }
        tooltip.add(Text.literal("击杀计数: " + kills + " / 10").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("右键：死亡斩击（冷却3秒）").formatted(Formatting.DARK_PURPLE));
    }

    public static void performSlashAttack(World world, ServerPlayerEntity player, ItemStack stack) {
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        float damage = getTotalAttackDamage(stack);

        double reach = 8.0;
        double width = 5.0;
        double height = 3.0;
        Vec3d center = eyePos.add(look.multiply(reach / 2));
        Box slashBox = new Box(
                center.x - width / 2, center.y - height / 2, center.z - width / 2,
                center.x + width / 2, center.y + height / 2, center.z + width / 2
        );

        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                slashBox,
                entity -> entity != player && entity.isAlive()
        );

        for (LivingEntity target : targets) {
            Vec3d toTarget = target.getPos().subtract(eyePos).normalize();
            double angle = Math.acos(look.dotProduct(toTarget));
            if (angle < Math.PI / 3) {
                target.damage(world.getDamageSources().playerAttack(player), damage);
            }
        }
    }

    public static void spawnSlashParticles(PlayerEntity player) {
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);

        Vec3d forward = look.normalize();
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up).normalize();
        if (right.lengthSquared() < 0.001) {
            right = new Vec3d(1, 0, 0);
        }
        Vec3d realUp = right.crossProduct(forward).normalize();

        int count = 80;
        double minDistance = 2.0;
        double maxDistance = 6.0;
        double maxAngle = Math.PI / 3;

        for (int i = 0; i < count; i++) {
            double angle = (world.random.nextDouble() - 0.5) * 2 * maxAngle;
            double distance = minDistance + world.random.nextDouble() * (maxDistance - minDistance);

            double horizontalOffset = Math.sin(angle) * distance;
            double forwardOffset = Math.cos(angle) * distance;

            Vec3d basePos = eyePos.add(forward.multiply(forwardOffset));
            Vec3d offset = right.multiply(horizontalOffset);

            double verticalSpread = 0.5;
            Vec3d verticalOffset = realUp.multiply((world.random.nextDouble() - 0.5) * verticalSpread);

            Vec3d pos = basePos.add(offset).add(verticalOffset);

            double spread = 0.15;
            pos = pos.add(
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread,
                    (world.random.nextDouble() - 0.5) * spread
            );

            if (i % 2 == 0) {
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
            } else {
                world.addParticle(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
    }

    private static final UUID TLIPOCA_HEALTH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    public static void applyMaxHealthBoost(ServerPlayerEntity player) {
        var attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(TLIPOCA_HEALTH_UUID);
            attribute.addPersistentModifier(new EntityAttributeModifier(
                    TLIPOCA_HEALTH_UUID,
                    "Tlipoca Health Boost",
                    0.3,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    public static void removeMaxHealthBoost(ServerPlayerEntity player) {
        var attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(TLIPOCA_HEALTH_UUID);
        }
    }
}