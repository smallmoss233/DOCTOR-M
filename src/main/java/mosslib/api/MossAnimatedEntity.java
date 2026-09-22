package mosslib.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface MossAnimatedEntity extends MossBedrockRenderable {

    Entity asEntity();

    /** 主动画 ID。null 表示这一帧不播。 */
    @Nullable
    Identifier getMossAnimationId();

    /** 主动画已播放的毫秒数。 */
    long getAnimationElapsedMs();

    /**
     * 叠加动画 ID —— 在主动画之上叠加。
     * <p>典型用途：待机姿势为底，偶发动作（转头、伸懒腰）叠加其上。</p>
     * <p>返回 null 表示没有叠加。默认 null。</p>
     */
    @Nullable
    default Identifier getMossOverlayAnimationId() {
        return null;
    }

    /** 叠加动画已播放的毫秒数。默认 0。 */
    default long getMossOverlayElapsedMs() {
        return 0L;
    }
}