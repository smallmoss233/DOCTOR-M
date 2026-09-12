package doctor_m.client.util.stp;

import net.minecraft.client.world.ClientWorld;

/** 由 {@code MinecraftClientMixin} 实现，提供无加载屏幕的世界切换。 */
public interface STPMinecraftClient {
    void stp$joinWorld(ClientWorld world);
}