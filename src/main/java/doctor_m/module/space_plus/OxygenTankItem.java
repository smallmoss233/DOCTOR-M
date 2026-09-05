package doctor_m.module.space_plus;

import doctor_m.config.ConfigManager;
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
    public static final String OXYGEN_KEY = "oxygen";
    private static final String START_TIME_KEY = "doctor_m_hold_start";

    public OxygenTankItem(Settings settings) {
        super(settings);
    }

    /** 获取当前物品实例的最大氧气容量，子类可重写此方法实现不同倍率 */
    public double getMaxOxygen() {
        return ConfigManager.getConfig().oxygenTankMaxOxygen;
    }

    public static double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    /**
     * 设置氧气量，自动根据物品类型获取对应的最大容量。
     * 如果是 {@link OxygenTankItem} 的子类，则使用其重写的 getMaxOxygen()。
     */
    public static void setOxygen(ItemStack stack, double amount) {
        double maxOxygen;
        if (stack.getItem() instanceof OxygenTankItem tankItem) {
            maxOxygen = tankItem.getMaxOxygen();
        } else {
            maxOxygen = ConfigManager.getConfig().oxygenTankMaxOxygen; // 回退值
        }
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, maxOxygen));
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

        var config = ConfigManager.getConfig();
        long startTime = nbt.getLong(START_TIME_KEY);
        long currentTime = world.getTime();
        long usedTicks = currentTime - startTime;
        nbt.remove(START_TIME_KEY);

        int holdThreshold = config.oxygenTankHoldTicksForAchievement;

        boolean canEat = canEatOxygenTank(player);
        if (canEat && usedTicks >= holdThreshold) {
            eatOxygenTank(player, stack);
            grantAdvancement(player, "you_ate_this");
            return;
        }

        if (usedTicks >= holdThreshold) {
            grantAdvancement(player, "not_thermos");
        }

        // 氧气补充逻辑（未食用时）
        ItemStack chestStack = player.getInventory().armor.get(2);
        if (!(chestStack.getItem() instanceof dev.amble.ait.module.planet.core.item.SpacesuitItem)) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.no_suit"), true);
            return;
        }

        double tankOxygen = getOxygen(stack);
        double suitOxygen = OxygenSystem.getOxygen(chestStack);

        if (tankOxygen <= 0) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.empty"), true);
            return;
        }

        double maxSuitOxygen = OxygenSystem.getMaxOxygen();
        if (suitOxygen >= maxSuitOxygen) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.suit_full"), true);
            return;
        }

        double transferRate = config.oxygenTankTransferRate;
        double transferAmount = Math.min(transferRate, tankOxygen);
        transferAmount = Math.min(transferAmount, maxSuitOxygen - suitOxygen);

        OxygenSystem.refillOxygen(chestStack, transferAmount);
        setOxygen(stack, tankOxygen - transferAmount);

        player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.transfer", transferAmount), true);

        if (getOxygen(stack) <= 0) {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.depleted"), true);
        }

        player.getItemCooldownManager().set(this, 5);
    }

    private boolean canEatOxygenTank(ServerPlayerEntity player) {
        var config = ConfigManager.getConfig();
        boolean hasStrength = player.hasStatusEffect(StatusEffects.STRENGTH);
        boolean hasHunger = player.hasStatusEffect(StatusEffects.HUNGER);
        boolean isStarving = player.getHungerManager().getFoodLevel() <= config.oxygenTankFoodThreshold;

        return hasStrength || hasHunger || isStarving;
    }

    private void eatOxygenTank(ServerPlayerEntity player, ItemStack stack) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30 * 20, 1, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20, 2, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30 * 20, 0, false, false, true));
        int newFood = Math.min(20, player.getHungerManager().getFoodLevel() + 10);
        player.getHungerManager().setFoodLevel(newFood);
        stack.decrement(1);
        player.playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
        player.getItemCooldownManager().set(this, 20);
    }

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
        var config = ConfigManager.getConfig();
        double oxygen = getOxygen(stack);
        double maxOxygen = getMaxOxygen(); // 使用实例方法，子类会返回正确倍率
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, maxOxygen));
        tooltip.add(Text.translatable("message.doctor_m.oxygen_tank", oxygen, maxOxygen));
    }
}