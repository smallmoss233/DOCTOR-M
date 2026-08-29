package doctor_m.module;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public final class PlayerTitleManager {

    private static final String KEY = "doctor_m:title";

    private PlayerTitleManager() {}

    public static String getTitle(PlayerEntity player) {
        if (player == null) return null;
        NbtCompound nbt = getCustomNbt(player);
        return nbt.contains(KEY) ? nbt.getString(KEY) : null;
    }

    public static void setTitle(PlayerEntity player, String title) {
        if (player == null) return;
        NbtCompound nbt = getCustomNbt(player);
        if (title == null || title.isBlank()) {
            nbt.remove(KEY);
        } else {
            nbt.putString(KEY, title);
        }
        markDirty(player);
    }

    public static void readFromNbt(PlayerEntity player, NbtCompound nbt) {
        NbtCompound custom = getCustomNbt(player);
        if (nbt.contains(KEY)) {
            custom.putString(KEY, nbt.getString(KEY));
        }
    }

    public static void writeToNbt(PlayerEntity player, NbtCompound nbt) {
        NbtCompound custom = getCustomNbt(player);
        if (custom.contains(KEY)) {
            nbt.putString(KEY, custom.getString(KEY));
        }
    }

    private static NbtCompound getCustomNbt(PlayerEntity player) {
        return ((CustomNbtAccessor) player).doctor_m$getCustomData();
    }

    private static void markDirty(PlayerEntity player) {
        ((CustomNbtAccessor) player).doctor_m$markDirty();
    }

    public interface CustomNbtAccessor {
        NbtCompound doctor_m$getCustomData();
        void doctor_m$markDirty();
    }
}