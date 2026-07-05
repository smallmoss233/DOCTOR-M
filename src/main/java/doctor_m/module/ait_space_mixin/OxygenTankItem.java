package doctor_m.module.ait_space_mixin;

import net.minecraft.advancement.Advancement;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public class OxygenTankItem extends Item {
    public static final String OXYGEN_KEY = "doctor_m_oxygen";
    public static final double MAX_OXYGEN = 1200.0;
    private static final double TRANSFER_RATE = 100.0;
    private static final String START_TIME_KEY = "doctor_m_hold_start";
    private static final int FOOD_THRESHOLD = 6; // 饱食度 <= 6 视为极低

    public OxygenTankItem(Settings settings) {
        super(settings);
    }

    public static double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    public static void setOxygen(ItemStack stack, double amount) {
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, MAX_OXYGEN));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }

        ItemStack stack = user.getStackInHand(hand);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(START_TIME_KEY, world.getTime());
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(START_TIME_KEY)) return;

        long startTime = nbt.getLong(START_TIME_KEY);
        long currentTime = world.getTime();
        long usedTicks = currentTime - startTime;
        nbt.remove(START_TIME_KEY);

        // ===== 1. 检测是否满足"食用"条件 =====
        boolean canEat = canEatOxygenTank(player);
        if (canEat && usedTicks >= 100) { // 必须长按5秒以上
            // 执行食用逻辑
            eatOxygenTank(player, stack);
            // 授予成就
            grantAdvancement(player, "you_ate_this");
            return; // 食用后不再执行氧气补充
        }

        // ===== 2. 检查是否达到5秒（用于成就“不是保温杯”） =====
        if (usedTicks >= 100) {
            grantAdvancement(player, "not_thermos");
        }

        // ===== 3. 执行氧气补充逻辑（仅当未食用） =====
        // 检查是否穿了宇航服
        ItemStack chestStack = player.getInventory().armor.get(2);
        if (!(chestStack.getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem)) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.no_suit"), true);
            return;
        }

        double tankOxygen = getOxygen(stack);
        double suitOxygen = SpaceOxygenManager.getOxygen(chestStack);

        if (tankOxygen <= 0) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.empty"), true);
            return;
        }

        if (suitOxygen >= SpaceOxygenManager.MAX_OXYGEN) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.suit_full"), true);
            return;
        }

        double transferAmount = Math.min(TRANSFER_RATE, tankOxygen);
        transferAmount = Math.min(transferAmount, SpaceOxygenManager.MAX_OXYGEN - suitOxygen);

        SpaceOxygenManager.refillOxygen(chestStack, transferAmount);
        setOxygen(stack, tankOxygen - transferAmount);

        player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.transfer", transferAmount), true);

        if (getOxygen(stack) <= 0) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.depleted"), true);
        }

        player.getItemCooldownManager().set(this, 5);
    }

    // ===== 判断是否可以食用 =====
    private boolean canEatOxygenTank(ServerPlayerEntity player) {
        // 拥有力量效果
        boolean hasStrength = player.hasStatusEffect(StatusEffects.STRENGTH);
        // 拥有饥饿效果
        boolean hasHunger = player.hasStatusEffect(StatusEffects.HUNGER);
        // 饱食度极低
        boolean isStarving = player.getHungerManager().getFoodLevel() <= FOOD_THRESHOLD;

        return hasStrength || hasHunger || isStarving;
    }

    // ===== 执行食用逻辑 =====
    private void eatOxygenTank(ServerPlayerEntity player, ItemStack stack) {
        // 1. 效果：抗性提升 II (等级1, 持续30秒)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30 * 20, 1, false, false, true));
        // 2. 效果：缺氧 -> 饥饿效果 (等级2, 持续30秒) + 虚弱 (等级1, 持续30秒) 模拟缺氧
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20, 2, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30 * 20, 0, false, false, true));
        // 3. 回复10点饱食度
        int newFood = Math.min(20, player.getHungerManager().getFoodLevel() + 10);
        player.getHungerManager().setFoodLevel(newFood);
        // 4. 消耗一个氧气瓶（减少数量）
        stack.decrement(1);
        // 5. 播放吃的声音和粒子（可选）
        player.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        // 6. 冷却
        player.getItemCooldownManager().set(this, 20);
    }

    // ===== 授予成就 =====
    private void grantAdvancement(ServerPlayerEntity player, String advancementId) {
        Advancement advancement = player.getServer().getAdvancementLoader()
                .get(new Identifier("doctor_m", advancementId));
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "has_held_long");
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        double oxygen = getOxygen(stack);
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, MAX_OXYGEN));
        tooltip.add(Text.translatable("message.doctor_m.oxygen_tank", oxygen, MAX_OXYGEN));
    }
}