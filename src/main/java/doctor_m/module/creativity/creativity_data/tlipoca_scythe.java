package doctor_m.module.creativity.creativity_data;

import com.google.common.collect.Multimap;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

import doctor_m.util.ScytheGrowthManager;
import doctor_m.util.ScytheSlashManager;

public class tlipoca_scythe extends SwordItem {
    public static final String KILL_COUNT_KEY = "TlipocaKillCount";
    public static final String GROWTH_KEY = "TlipocaGrowth";
    private static final String INIT_KEY = "TlipocaInit";

    private static final UUID DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789014");
    private static final UUID SPEED_UUID = UUID.fromString("12345678-1234-1234-1234-123456789016");
    public static final UUID TLIPOCA_HEALTH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    // 单例引用（用于冷却管理）
    private static tlipoca_scythe INSTANCE;
    public static tlipoca_scythe getInstance() { return INSTANCE; }

    public tlipoca_scythe(Settings settings) {
        super(TlipocaMaterial.INSTANCE, 0, -3.2f, settings);
        INSTANCE = this;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return super.getAttributeModifiers(slot);
    }

    /**
     * 初始化物品NBT - 只执行一次，安全可重入
     */
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

        // 只在没有AttributeModifiers时初始化
        if (!nbt.contains("AttributeModifiers", 9)) {
            int growth = nbt.getInt(GROWTH_KEY);
            writeAttributeModifiers(stack, 20.0f + growth);
            changed = true;
        }

        if (!nbt.contains(GROWTH_KEY, 3)) {
            nbt.putInt(GROWTH_KEY, 0);
            changed = true;
        }
        if (!nbt.contains(KILL_COUNT_KEY, 3)) {
            nbt.putInt(KILL_COUNT_KEY, 0);
            changed = true;
        }

        if (changed) {
            System.out.println("[TlipocaScythe] NBT initialized. Damage=" + (20 + nbt.getInt(GROWTH_KEY)));
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

    /**
     * 【关键】inventoryTick只做一次性初始化，不做任何检查或更新
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.getBoolean(INIT_KEY)) {
            nbt.putBoolean(INIT_KEY, true);
            initScytheNbt(stack);
        }
    }

    /**
     * 写入AttributeModifiers - 只在初始化或成长升级时调用
     */
    public static void writeAttributeModifiers(ItemStack stack, float damage) {
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
        System.out.println("[TlipocaScythe] AttributeModifiers written: damage=" + damage);
    }

    /**
     * 右键使用 - 调用斩击工具类
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            // 服务端：执行实际斩击伤害
            if (user instanceof ServerPlayerEntity serverPlayer) {
                ScytheSlashManager.performSlash((net.minecraft.server.world.ServerWorld) world, serverPlayer, stack);
            }
        } else {
            // 客户端：只生成粒子
            ScytheSlashManager.spawnParticlesClient(user);
        }

        return TypedActionResult.success(stack);
    }

    // ========== 生命值加成 ==========

    public static void applyMaxHealthBoost(ServerPlayerEntity player) {
        var attribute = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
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
        var attribute = player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(TLIPOCA_HEALTH_UUID);
        }
    }

    // ========== Tooltip ==========

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int growth = stack.getOrCreateNbt().getInt(GROWTH_KEY);
        int kills = stack.getOrCreateNbt().getInt(KILL_COUNT_KEY);
        int killsToNext = ScytheGrowthManager.getKillsToNext(stack);

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
}