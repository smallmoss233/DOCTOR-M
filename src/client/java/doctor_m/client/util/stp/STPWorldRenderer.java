package doctor_m.client.util.stp;

import net.minecraft.client.world.ClientWorld;

/** 由 {@code WorldRendererMixin} 实现，提供轻量级世界切换。 */
public interface STPWorldRenderer {
    void stp$setWorld(ClientWorld world);
}