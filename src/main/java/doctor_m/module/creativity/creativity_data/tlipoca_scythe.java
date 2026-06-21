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

    // 注意：这些UUID必须与NBT中的UUID一致
    private static final UUID DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789014");
    private static final UUID SPEED_UUID = UUID.fromString("12345678-1234-1234-1234-123456789016");
    private static final UUID TLIPOCA_HEALTH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    public tlipoca_scythe(Settings settings) {
        super(TlipocaMaterial.INSTANCE, 0, -3.2f, settings);
    }

    /**
     * 【关键修复】这里必须返回包含攻击伤害和攻击速度的修饰符
     * 原来返回空Multimap导致武器没有任何伤害属性
     */
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            // 返回动态计算的属性，基于当前ItemStack的NBT中的伤害值
            // 注意：这里无法直接访问ItemStack，所以需要在inventoryTick中确保NBT正确
            // 基础返回父类的，但会被NBT中的AttributeModifiers覆盖
            return super.getAttributeModifiers(slot);
        }
        return super.getAttributeModifiers(slot);
    }

    public static void initScytheNbt(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        boolean changed = false;

        // 初始化锋利10
        if (EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) == 0) {
            stack.addEnchantment(Enchantments.SHARPNESS, 10);
            changed = true;
        }
        // 初始化抢夺10
        if (EnchantmentHelper.getLevel(Enchantments.LOOTING, stack) == 0) {
            stack.addEnchantment(Enchantments.LOOTING, 10);
            changed = true;
        }
        // 初始化无法破坏
        if (!nbt.getBoolean("Unbreakable")) {
            nbt.putBoolean("Unbreakable", true);
            changed = true;
        }

        // 初始化属性修饰符（如果还没有的话）
        if (!nbt.contains("AttributeModifiers", 9)) {
            // 默认20点伤害
            writeAttributeModifiers(stack, 20.0f);
            changed = true;
        }

        // 确保成长值存在
        if (!nbt.contains(GROWTH_KEY)) {
            nbt.putInt(GROWTH_KEY, 0);
        }
        // 确保击杀数存在
        if (!nbt.contains(KILL_COUNT_KEY)) {
            nbt.putInt(KILL_COUNT_KEY, 0);
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
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        // 只初始化一次
        if (!nbt.getBoolean(INIT_KEY)) {
            nbt.putBoolean(INIT_KEY, true);
            initScytheNbt(stack);
        }

        // 【关键修复】确保AttributeModifiers始终与当前成长值同步
        // 因为玩家可能通过命令或其他方式修改了NBT
        int growth = nbt.getInt(GROWTH_KEY);
        float expectedDamage = 20.0f + growth;

        // 检查当前AttributeModifiers中的伤害值是否正确
        if (nbt.contains("AttributeModifiers", 9)) {
            NbtList list = nbt.getList("AttributeModifiers", 10);
            boolean needUpdate = true;
            for (int i = 0; i < list.size(); i++) {
                NbtCompound modifier = list.getCompound(i);
                if ("minecraft:generic.attack_damage".equals(modifier.getString("AttributeName"))) {
                    float currentAmount = modifier.getFloat("Amount");
                    // 允许一点浮点误差
                    if (Math.abs(currentAmount - expectedDamage) < 0.01f) {
                        needUpdate = false;
                    }
                    break;
                }
            }
            if (needUpdate) {
                writeAttributeModifiers(stack, expectedDamage);
            }
        }
    }

    private static void writeAttributeModifiers(ItemStack stack, float damage) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList list = new NbtList();

        // 攻击伤害修饰符
        NbtCompound dmg = new NbtCompound();
        dmg.putString("AttributeName", "minecraft:generic.attack_damage");
        dmg.putString("Name", "tlipoca_damage");
        dmg.putDouble("Amount", damage);
        dmg.putInt("Operation", 0); // ADDITION
        dmg.putUuid("UUID", DAMAGE_UUID);
        dmg.putString("Slot", "mainhand");
        list.add(dmg);

        // 攻击速度修饰符
        NbtCompound spd = new NbtCompound();
        spd.putString("AttributeName", "minecraft:generic.attack_speed");
        spd.putString("Name", "tlipoca_speed");
        spd.putDouble("Amount", -3.6); // 基础-3.2 + (-3.6) = 很慢的攻击速度，根据需要调整
        spd.putInt("Operation", 0); // ADDITION
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

    /**
     * 【关键修复】成长逻辑
     * 问题：原来通过findScytheInInventory查找，但击杀时主手可能不是镰刀
     * 修复：直接传入ItemStack，确保操作的是正确的物品
     */
    public static void onKill(ItemStack stack, ServerPlayerEntity player) {
        if (!(stack.getItem() instanceof tlipoca_scythe)) {
            return; // 安全检查
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        int killCount = nbt.getInt(KILL_COUNT_KEY) + 1;
        nbt.putInt(KILL_COUNT_KEY, killCount);
        System.out.println("[TlipocaScythe] Kill! Count=" + killCount);

        // 每10个击杀增加5点伤害
        if (killCount % 10 == 0) {
            int growth = nbt.getInt(GROWTH_KEY) + 5;
            nbt.putInt(GROWTH_KEY, growth);
            float newDamage = 20.0f + growth;

            // 更新属性修饰符
            writeAttributeModifiers(stack, newDamage);

            // 标记背包需要同步
            player.getInventory().markDirty();

            // 发送成长提示
            player.sendMessage(Text.literal("§6☠ 特斯卡特利波卡之镰已吸收灵魂，攻击力提升至 " + newDamage + "！"), false);

            System.out.println("[TlipocaScythe] GROWTH UP! New damage=" + newDamage);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int growth = stack.getOrCreateNbt().getInt(GROWTH_KEY);
        int kills = stack.getOrCreateNbt().getInt(KILL_COUNT_KEY);
        int killsToNext = 10 - (kills % 10);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("╔══ 特斯卡特利波卡之镰 ══╗").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("  基础伤害: 20").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  当前伤害: " + (20 + growth)).formatted(Formatting.DARK_RED, Formatting.BOLD));
        if (growth > 0) {
            tooltip.add(Text.literal("  成长加成: +" + growth).formatted(Formatting.GREEN));
        }
        tooltip.add(Text.literal("  击杀计数: " + kills + " (还需 " + killsToNext + " 个升级)").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("  §7✦ 最大生命值 +30%").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("  §7✦ 锋利 X · 抢夺 X").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("  §7✦ 无法破坏").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("  高血量 (>50%): 力量V 幸运V").formatted(Formatting.RED));
        tooltip.add(Text.literal("  低血量 (<50%): 力量V 抗性V 恢复V").formatted(Formatting.BLUE));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("  右键：§c死亡斩击 §7(冷却3秒)").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("  攻击附带：§4所有原版负面效果 §7(等级X)").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("╚════════════════════╝").formatted(Formatting.DARK_PURPLE));
    }

    public static void performSlashAttack(World world, ServerPlayerEntity player, ItemStack stack) {
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        float damage = getTotalAttackDamage(stack);

        // 扇形范围参数
        double reach = 8.0;      // 最远距离
        double width = 5.0;      // 宽度
        double height = 3.0;     // 高度
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

        int hitCount = 0;
        for (LivingEntity target : targets) {
            Vec3d toTarget = target.getPos().subtract(eyePos).normalize();
            double angle = Math.acos(look.dotProduct(toTarget));
            // 60度扇形
            if (angle < Math.PI / 3) {
                target.damage(world.getDamageSources().playerAttack(player), damage);
                hitCount++;
            }
        }

        if (hitCount > 0) {
            player.sendMessage(Text.literal("§7斩击命中 §c" + hitCount + " §7个目标"), true);
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

    // ========== 生命值加成 ==========

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