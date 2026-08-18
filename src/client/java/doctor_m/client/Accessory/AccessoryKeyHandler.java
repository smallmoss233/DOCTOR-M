package doctor_m.client.Accessory;

import net.minecraft.entity.player.PlayerEntity;

public interface AccessoryKeyHandler {
    /** 优先级，数字越小越优先执行 */
    int getPriority();

    /** 玩家当前是否满足触发条件（自己决定扫描范围） */
    boolean isActive(PlayerEntity player);

    /** Z 键按下时的行为 */
    void onSkillKey(PlayerEntity player);

    /** X 键按下时的行为 */
    void onCoreKey(PlayerEntity player);

    /** 执行后是否阻止更低优先级的 handler 继续执行 */
    boolean blocksOthers();
}