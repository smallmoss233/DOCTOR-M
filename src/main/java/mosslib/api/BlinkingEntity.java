package mosslib.api;

/** 支持代码驱动眨眼的实体。 */
public interface BlinkingEntity {
    /** 当前是否处于眨眼动画中。 */
    boolean isBlinking();

    /** 睁眼倍率：1.0 = 完全睁开，0.1 = 完全闭合。 */
    float getBlinkScale();
}