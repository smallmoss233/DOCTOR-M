package doctor_m.Item.data_item.KeytoTimeFragment;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.KeytoTime;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class RelicGemItem extends TrinketItem implements KeytoTime {

    // ========== NBT 常量 ==========
    private static final String NBT_LEVEL = "GemLevel";
    private static final String NBT_COOLDOWN_TICKS = "CooldownUntilTick";

    public static final int MAX_LEVEL = 3;

    private static final int BASE_COOLDOWN_TICKS = 20 * 60 * 10;          // 10分钟
    private static final int COOLDOWN_REDUCTION_PER_LEVEL = 20 * 60 * 2;   // 每级减2分钟

    // ========== 构造 ==========
    public RelicGemItem(Settings settings) {
        super(settings.maxCount(1));
    }

    // ========== NBT 读写 ==========
    public static int getLevel(ItemStack stack) {
        return stack.getOrCreateNbt().getInt(NBT_LEVEL);
    }

    public static void setLevel(ItemStack stack, int level) {
        stack.getOrCreateNbt().putInt(NBT_LEVEL, Math.min(level, MAX_LEVEL));
    }

    public static long getCooldownUntilTick(ItemStack stack) {
        return stack.getOrCreateNbt().getLong(NBT_COOLDOWN_TICKS);
    }

    public static void setCooldownUntilTick(ItemStack stack, long tick) {
        stack.getOrCreateNbt().putLong(NBT_COOLDOWN_TICKS, tick);
    }

    public static boolean isOnCooldown(ItemStack stack, long worldTime) {
        return worldTime < getCooldownUntilTick(stack);
    }

    // ========== 属性计算 ==========
    public static int getPassiveResistanceLevel(int gemLevel) {
        return Math.min(1 + gemLevel, 4);
    }

    public static int getPassiveResistanceLevel(ItemStack stack) {
        return getPassiveResistanceLevel(getLevel(stack));
    }

    public static int getXpCost(int targetLevel) {
        return switch (targetLevel) {
            case 1 -> 120;
            case 2 -> 240;
            case 3 -> 480;
            default -> 0;
        };
    }

    public static int getCooldownTicks(int gemLevel) {
        return Math.max(20 * 60, BASE_COOLDOWN_TICKS - gemLevel * COOLDOWN_REDUCTION_PER_LEVEL);
    }

    public static int getActiveTicks(int gemLevel) {
        return 20 * (15 + gemLevel * 10);
    }

    // ========== 被动效果管理 ==========
    private static void applyPassiveResistance(PlayerEntity player, ItemStack stack) {
        int level = getPassiveResistanceLevel(stack);
        // 移除旧抗性（如果有）再添加，确保等级更新
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                Integer.MAX_VALUE,   // 无限持续时间
                level - 1,
                false, false, false
        ));
    }

    private static void removePassiveResistance(PlayerEntity player) {
        player.removeStatusEffect(StatusEffects.RESISTANCE);
    }

    // ========== 装备/卸下 ==========
    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        if (entity instanceof PlayerEntity player && !player.getWorld().isClient()) {
            // 装备时如果不在冷却，立即施加被动抗性
            if (!isOnCooldown(stack, player.getWorld().getTime())) {
                applyPassiveResistance(player, stack);
            }
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onUnequip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            removePassiveResistance(player);
            // 注意：主动 BUFF 由复活事件管理，这里只移除被动抗性
        }
    }

    // ========== 每 Tick 更新（由外部调用） ==========
    public static void tick(PlayerEntity player, ItemStack stack) {
        if (player.getWorld().isClient()) return;
        long worldTime = player.getWorld().getTime();

        if (isOnCooldown(stack, worldTime)) {
            // 冷却中，确保被动抗性被移除（防止复活血量低但残留被动）
            removePassiveResistance(player);
            return;
        }

        // 冷却结束，施加被动抗性（如果缺失或等级不符，applyPassiveResistance 会处理）
        applyPassiveResistance(player, stack);
    }

    // ========== 右键升级 ==========
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) return TypedActionResult.pass(stack);

        int currentLevel = getLevel(stack);
        if (currentLevel >= MAX_LEVEL) {
            user.sendMessage(Text.translatable("message.doctor_m.relic_gem.max_level").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        int cost = getXpCost(currentLevel + 1);
        if (user.experienceLevel < cost) {
            user.sendMessage(Text.translatable("message.doctor_m.relic_gem.not_enough_xp", cost).formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        user.addExperienceLevels(-cost);
        setLevel(stack, currentLevel + 1);

        // 如果已装备，立即刷新被动抗性
        TrinketsApi.getTrinketComponent(user).ifPresent(comp -> {
            boolean equipped = comp.getEquipped(s -> s == stack).stream().findAny().isPresent();
            if (equipped) {
                applyPassiveResistance(user, stack);
            }
        });

        user.sendMessage(Text.translatable("message.doctor_m.relic_gem.level_up", currentLevel + 1).formatted(Formatting.GREEN), true);
        world.playSound(null, user.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.5f);

        return TypedActionResult.success(stack);
    }

    // ========== 工具提示 ==========
    @Override
    public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        int level = getLevel(stack);
        int resistance = getPassiveResistanceLevel(stack);

        tooltip.add(Text.translatable("message.doctor_m.relic_gem").formatted(Formatting.GRAY));

        String statusKey;
        Object[] args;

        if (world != null && isOnCooldown(stack, world.getTime())) {
            long remainTicks = getCooldownUntilTick(stack) - world.getTime();
            long remainSec = remainTicks / 20;
            long minutes = remainSec / 60;
            long seconds = remainSec % 60;
            statusKey = "message.doctor_m.relic_gem.status.cooldown";
            args = new Object[]{level, MAX_LEVEL, resistance, minutes, seconds};
        } else if (level < MAX_LEVEL) {
            statusKey = "message.doctor_m.relic_gem.status.upgradable";
            args = new Object[]{level, MAX_LEVEL, resistance, getXpCost(level + 1)};
        } else {
            statusKey = "message.doctor_m.relic_gem.status.max";
            args = new Object[]{level, MAX_LEVEL, resistance};
        }

        tooltip.add(Text.translatable(statusKey, args).formatted(Formatting.AQUA));
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.relic_gem.detail")
        );
    }
}