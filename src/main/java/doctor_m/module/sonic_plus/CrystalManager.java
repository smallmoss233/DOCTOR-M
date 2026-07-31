package doctor_m.module.sonic_plus;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class CrystalManager {
    public static final Identifier DEFAULT_CRYSTAL = new Identifier("ait", "zeiton_shard");
    public static final Identifier AMETHYST_CRYSTAL = new Identifier("minecraft", "amethyst_shard");

    private static final String CRYSTAL_KEY = "doctor_m.installed_crystal";

    public static Identifier getInstalledCrystal(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(CRYSTAL_KEY)) {
            return DEFAULT_CRYSTAL;
        }
        String id = nbt.getString(CRYSTAL_KEY);
        Identifier parsed = Identifier.tryParse(id);
        return parsed != null ? parsed : DEFAULT_CRYSTAL;
    }

    public static void setInstalledCrystal(ItemStack stack, Identifier crystalId) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (crystalId.equals(DEFAULT_CRYSTAL)) {
            nbt.remove(CRYSTAL_KEY);
        } else {
            nbt.putString(CRYSTAL_KEY, crystalId.toString());
        }
    }

    public static boolean isValidCrystal(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id.equals(AMETHYST_CRYSTAL) || id.equals(DEFAULT_CRYSTAL);
    }

    public static ItemStack createCrystalStack(Identifier id) {
        Item item = Registries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}