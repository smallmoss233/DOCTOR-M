package mosslib.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 实体实现这个接口就能被 MossBedrockEntityRenderer 渲染并播动画。
 * 只暴露 Identifier，不碰 MossAnimation —— 后者在客户端源码集。
 */
public interface MossAnimatedEntity extends MossBedrockRenderable {

    Entity asEntity();

    /** 当前动画 ID。返回 null 表示这一帧不播 Bedrock 动画，直接走原版回退。 */
    @Nullable
    Identifier getMossAnimationId();

    /** 当前动画已播放的毫秒数。 */
    long getAnimationElapsedMs();

    /**
     * Bedrock 动画缺失时，是否回退到 Minecraft 原版的肢体动画（走路摆臂 + 头部跟随）。
     * 默认 true。返回 false 表示宁可站着不动，也不走原版。
     */
    default boolean useVanillaFallback() {
        return true;
    }
}