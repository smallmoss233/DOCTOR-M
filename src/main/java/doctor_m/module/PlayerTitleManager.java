package doctor_m.module;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

/**
 * 服务端称号管理
 * 称号保存在玩家 NBT 的 doctor_m:title 字段
 */
public final class PlayerTitleManager {

    private static final String KEY = "doctor_m:title";

    private PlayerTitleManager() {}

    /** 获取玩家称号（服务端/客户端通用，但客户端建议用缓存） */
    public static String getTitle(PlayerEntity player) {
        if (player == null) return null;
        NbtCompound nbt = getCustomNbt(player);
        return nbt.contains(KEY) ? nbt.getString(KEY) : null;
    }

    /** 设置玩家称号（仅服务端调用） */
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

    /** 从 NBT 读取（玩家登录时调用） */
    public static void readFromNbt(PlayerEntity player, NbtCompound nbt) {
        NbtCompound custom = getCustomNbt(player);
        if (nbt.contains(KEY)) {
            custom.putString(KEY, nbt.getString(KEY));
        }
    }

    /** 写入 NBT（玩家保存时调用） */
    public static void writeToNbt(PlayerEntity player, NbtCompound nbt) {
        NbtCompound custom = getCustomNbt(player);
        if (custom.contains(KEY)) {
            nbt.putString(KEY, custom.getString(KEY));
        }
    }

    // ========== 内部辅助 ==========

    private static NbtCompound getCustomNbt(PlayerEntity player) {
        // 通过 accessor 获取或创建自定义 NBT
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