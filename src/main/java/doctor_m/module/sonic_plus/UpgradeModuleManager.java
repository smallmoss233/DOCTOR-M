package doctor_m.module.sonic_plus;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * 音速起子升级模块的 NBT 管理。
 * 两种模块互斥，只能同时存在一种。
 */
public class UpgradeModuleManager {

    public static final String UPGRADE_MODULE_KEY = "doctor_m.upgrade_module";

    public static final Identifier EMPTY = new Identifier("minecraft", "air");
    public static final Identifier ENERGY_UPGRADE = new Identifier("doctor_m", "energy_upgrade_module");
    public static final Identifier REGENERATION_MODULE = new Identifier("doctor_m", "regeneration_module");

    /** 扩容模块能量上限（可在模组配置里改这个值） */
    public static double ENERGY_UPGRADE_MAX = 2000.0;
    /** 再生模块能量上限（可在模组配置里改这个值） */
    public static double REGENERATION_MAX = 500.0;
    /** 再生模块每 tick 恢复量（默认 0.5，20 tick = 1秒回 10 点） */
    public static double REGENERATION_RATE = 0.5;

    public static boolean isValidUpgradeModule(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id.equals(ENERGY_UPGRADE) || id.equals(REGENERATION_MODULE);
    }

    public static Identifier getInstalledModule(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        String rawId = nbt.getString(UPGRADE_MODULE_KEY);
        if (rawId == null || rawId.isEmpty()) return EMPTY;
        Identifier id = Identifier.tryParse(rawId);
        return id == null ? EMPTY : id;
    }

    public static void setInstalledModule(ItemStack stack, Identifier id) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (id == null || id.equals(EMPTY)) {
            nbt.remove(UPGRADE_MODULE_KEY);
        } else {
            nbt.putString(UPGRADE_MODULE_KEY, id.toString());
        }
    }

    public static ItemStack createModuleStack(Identifier id) {
        if (id == null || id.equals(EMPTY)) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    public static boolean hasEnergyUpgrade(ItemStack stack) {
        return getInstalledModule(stack).equals(ENERGY_UPGRADE);
    }

    public static boolean hasRegenerationModule(ItemStack stack) {
        return getInstalledModule(stack).equals(REGENERATION_MODULE);
    }
}