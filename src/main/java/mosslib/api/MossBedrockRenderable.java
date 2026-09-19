package mosslib.api;

import net.minecraft.util.Identifier;

/**
 * 任何想让 MossBedrock 渲染的方块实体都实现这个接口。
 * 只有客户端渲染会调用这些方法，服务端实现可以返回 null 或默认值。
 */
public interface MossBedrockRenderable {

    /** 模型 ID，对应 assets/<namespace>/bedrock/<path>.geo.json 里的 <namespace>:<path> */
    Identifier getMossModel();

    /** 主纹理 */
    Identifier getMossTexture();

    /** 发光层纹理，没有返回 null */
    default Identifier getMossEmission() {
        return null;
    }

    /** Y 轴朝向（度） */
    default float getMossYaw(float tickDelta) {
        return 0f;
    }

    /** 动画时间轴（tick） */
    default float getMossAge(float tickDelta) {
        return 0f;
    }
}