package mosslib.api;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 方块实体实现这个接口就能被 MossBedrockRenderer 播动画。
 * 只暴露 Identifier，不碰 MossAnimation —— 后者是客户端资源。
 */
public interface MossAnimatedBlockEntity {

    BlockEntity be();

    /** 当前动画的 ID。null 表示静止。 */
    @Nullable
    Identifier getMossAnimationId();

    /** 动画已播放的毫秒数。 */
    long getAnimationElapsedMs();
}