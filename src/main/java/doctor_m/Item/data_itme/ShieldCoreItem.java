package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import doctor_m.config.ConfigManager;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class ShieldCoreItem extends TrinketItem {

    private static final String ENERGY_KEY = "Energy";

    public ShieldCoreItem(Settings settings) {
        super(settings);
    }

    // ========== 创造模式默认满能量 ==========
    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        // 直接写入满能量，创造模式物品栏会显示满耐久条
        setEnergy(stack, getMaxEnergy());
        return stack;
    }

    // ========== 配置代理 ==========
    public static int getMaxEnergy() {
        return ConfigManager.getConfig().shieldMaxEnergy;
    }

    public static int getRechargePerTick() {
        return ConfigManager.getConfig().shieldRechargePerTick;
    }

    public static int getCostPerDamage() {
        return ConfigManager.getConfig().shieldCostPerDamage;
    }

    // ========== 能量条 ==========
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round((float) getEnergy(stack) * 13.0F / (float) getMaxEnergy());
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x00FFFF;
    }

    // ========== 充能逻辑 ==========
    /**
     * 饰品槽位充能（装备在 Trinkets 槽时触发）
     */
    @Override
    public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
        if (!entity.getWorld().isClient && entity instanceof PlayerEntity) {
            recharge(stack);
        }
    }

    /**
     * 背包/手持充能（在原版物品栏里时触发）
     * 注意：如果物品已装备在 Trinkets 槽，原版不会遍历到它，不会重复充能。
     */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity) {
            recharge(stack);
        }
    }

    private void recharge(ItemStack stack) {
        int energy = getEnergy(stack);
        int max = getMaxEnergy();
        if (energy >= max) return; // 提前返回，避免无意义计算
        setEnergy(stack, Math.min(energy + getRechargePerTick(), max));
    }

    // ========== NBT（只读用 getNbt，避免空 Tag 污染客户端同步） ==========
    public static int getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getInt(ENERGY_KEY) : 0;
    }

    public static void setEnergy(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(amount, getMaxEnergy()));
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getInt(ENERGY_KEY) != clamped) {
            nbt.putInt(ENERGY_KEY, clamped);
        }
    }

    public static boolean consumeEnergy(ItemStack stack, int amount) {
        if (stack.isEmpty()) return false;
        int energy = getEnergy(stack);
        if (energy < amount) return false;
        setEnergy(stack, energy - amount);
        return true;
    }

    // ========== 提示 ==========
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("message.doctor_m.shield_core.energy",
                getEnergy(stack), getMaxEnergy()).formatted(Formatting.GRAY));

        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.shield_core.detail")
        );
    }
}