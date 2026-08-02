package doctor_m.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Client 侧 Screen 打开的回调桥接
 * 由 src/client 的 Client Entrypoint 初始化
 */
public class VMClientScreenOpener {
    public static Opener opener;

    @FunctionalInterface
    public interface Opener {
        void open(PlayerEntity player, ItemStack stack);
    }
}