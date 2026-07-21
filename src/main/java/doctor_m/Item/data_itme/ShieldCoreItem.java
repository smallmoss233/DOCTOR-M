package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import doctor_m.config.ConfigManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class ShieldCoreItem extends TrinketItem {

    public ShieldCoreItem(Settings settings) {
        super(settings);
    }

    // ===== 配置值的便捷访问方法（改为 public） =====
    public static int getMaxEnergy() {
        return ConfigManager.getConfig().shieldMaxEnergy;
    }

    public static int getRechargePerTick() {
        return ConfigManager.getConfig().shieldRechargePerTick;
    }

    public static int getCostPerDamage() {
        return ConfigManager.getConfig().shieldCostPerDamage;
    }

    // ===== 充能逻辑 =====
    @Override
    public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
        if (!entity.getWorld().isClient && entity instanceof PlayerEntity) {
            recharge(stack);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity) {
            recharge(stack);
        }
    }

    private void recharge(ItemStack stack) {
        int energy = getEnergy(stack);
        int maxEnergy = getMaxEnergy();
        int rechargeRate = getRechargePerTick();
        if (energy < maxEnergy) {
            setEnergy(stack, Math.min(energy + rechargeRate, maxEnergy));
        }
    }

    // ===== 静态工具方法 =====
    public static int getEnergy(ItemStack stack) {
        return stack.getOrCreateNbt().getInt("Energy");
    }

    public static void setEnergy(ItemStack stack, int amount) {
        int maxEnergy = getMaxEnergy();
        stack.getOrCreateNbt().putInt("Energy", Math.max(0, Math.min(amount, maxEnergy)));
    }

    public static boolean consumeEnergy(ItemStack stack, int amount) {
        int energy = getEnergy(stack);
        if (energy < amount) return false;
        setEnergy(stack, energy - amount);
        return true;
    }

    // ===== 能量提示 =====
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        int maxEnergy = getMaxEnergy();
        tooltip.add(Text.translatable("message.doctor_m.shield_core.energy", getEnergy(stack), maxEnergy)
                .formatted(Formatting.GRAY));
    }
}