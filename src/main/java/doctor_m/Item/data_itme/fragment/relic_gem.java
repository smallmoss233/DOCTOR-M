package doctor_m.Item.data_itme.fragment;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
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

public class relic_gem extends TrinketItem {

    private static final String NBT_LEVEL = "GemLevel";
    private static final String NBT_COOLDOWN = "CooldownUntil";

    public static final int MAX_LEVEL = 3;
    private static final int BASE_COOLDOWN_TICKS = 20 * 60 * 10;
    private static final int COOLDOWN_REDUCTION_PER_LEVEL = 20 * 60 * 2;

    public relic_gem(Settings settings) {
        super(settings.maxCount(1));
    }

    // ========== NBT ==========

    public static int getLevel(ItemStack stack) {
        return stack.getOrCreateNbt().getInt(NBT_LEVEL);
    }

    public static void setLevel(ItemStack stack, int level) {
        stack.getOrCreateNbt().putInt(NBT_LEVEL, Math.min(level, MAX_LEVEL));
    }

    public static long getCooldownUntil(ItemStack stack) {
        return stack.getOrCreateNbt().getLong(NBT_COOLDOWN);
    }

    public static void setCooldownUntil(ItemStack stack, long time) {
        stack.getOrCreateNbt().putLong(NBT_COOLDOWN, time);
    }

    public static boolean isOnCooldown(ItemStack stack, long worldTime) {
        return worldTime < getCooldownUntil(stack);
    }

    // ========== 属性计算 ==========

    public static int getPassiveResistanceLevel(int level) {
        return Math.min(1 + level, 4);
    }

    public static int getPassiveResistanceLevel(ItemStack stack) {
        return getPassiveResistanceLevel(getLevel(stack));
    }

    public static int getXpCost(int targetLevel) {
        return new int[]{120, 240, 480}[targetLevel - 1];
    }

    public static int getCooldownTicks(int level) {
        return Math.max(20 * 60, BASE_COOLDOWN_TICKS - level * COOLDOWN_REDUCTION_PER_LEVEL);
    }

    public static int getActiveTicks(int level) {
        return 20 * (15 + level * 10);
    }

    // ========== 装备/卸下 ==========

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        if (entity instanceof PlayerEntity player && !player.getWorld().isClient()) {
            long worldTime = player.getWorld().getTime();
            if (!isOnCooldown(stack, worldTime)) {
                applyPassiveEffect(player, stack);
            }
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onUnequip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            removeAllGemEffects(player);
        }
    }

    public static void applyPassiveEffect(PlayerEntity player, ItemStack stack) {
        int level = getPassiveResistanceLevel(stack);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RESISTANCE,
                Integer.MAX_VALUE,
                level - 1,
                false, false, false
        ));
    }

    public static void removeAllGemEffects(PlayerEntity player) {
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.SPEED);
        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        player.removeStatusEffect(StatusEffects.HASTE);
        player.removeStatusEffect(StatusEffects.WATER_BREATHING);
    }

    // ========== 每 tick 更新 ==========

    public static void tick(PlayerEntity player, ItemStack stack) {
        if (player.getWorld().isClient()) return;
        long worldTime = player.getWorld().getTime();

        // ⭐ 冷却中直接返回，不施加被动抗性
        if (isOnCooldown(stack, worldTime)) {
            return;
        }

        // 冷却结束，施加被动抗性（如果玩家没有）
        if (!player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            applyPassiveEffect(player, stack);
        }
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

        TrinketsApi.getTrinketComponent(user).ifPresent(component -> {
            boolean isEquipped = component.getEquipped(stack2 -> stack2 == stack).stream().findAny().isPresent();
            if (isEquipped) {
                user.removeStatusEffect(StatusEffects.RESISTANCE);
                applyPassiveEffect(user, stack);
            }
        });

        user.sendMessage(Text.translatable("message.doctor_m.relic_gem.level_up", currentLevel + 1).formatted(Formatting.GREEN), true);
        world.playSound(null, user.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.5f);

        return TypedActionResult.success(stack);
    }

    // ========== 提示文本 ==========

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.world.World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        int level = getLevel(stack);
        int resistance = getPassiveResistanceLevel(stack);

        tooltip.add(Text.translatable("message.doctor_m.relic_gem").formatted(Formatting.GRAY));

        String statusKey;
        Object[] args;

        if (world != null && isOnCooldown(stack, world.getTime())) {
            long remain = (getCooldownUntil(stack) - world.getTime()) / 20;
            statusKey = "message.doctor_m.relic_gem.status.cooldown";
            args = new Object[]{level, MAX_LEVEL, resistance, remain / 60, remain % 60};
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