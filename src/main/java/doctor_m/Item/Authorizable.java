package doctor_m.Item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;

/**
 * 可被钥匙授权/收回授权的物品需实现此接口
 */
public interface Authorizable {

    /**
     * 检查该物品栈是否已授权
     */
    boolean isAuthorized(ItemStack stack);

    /**
     * 设置授权状态
     */
    void setAuthorized(ItemStack stack, boolean authorized);

    /**
     * 授权成功时播放的音效
     */
    SoundEvent getAuthorizeSound();

    /**
     * 收回授权时播放的音效
     */
    SoundEvent getRevokeSound();

    /**
     * 授权状态变更时的额外逻辑（可选，如粒子效果）
     * 默认空实现
     */
    default void onAuthorizationChanged(PlayerEntity player, ItemStack stack, boolean newState) {
        // 可被子类重写
    }
}