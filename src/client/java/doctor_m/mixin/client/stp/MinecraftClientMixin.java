package doctor_m.mixin.client.stp;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import dev.amble.ait.api.ClientWorldEvents;
import doctor_m.client.module.stp.STPMinecraftClient;
import doctor_m.module.STP;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ApiServices;
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements STPMinecraftClient {

    @Shadow @Nullable public ClientWorld world;
    @Shadow private boolean integratedServerRunning;
    @Shadow @Final private YggdrasilAuthenticationService authenticationService;
    @Shadow @Final public File runDirectory;
    @Shadow @Final public ParticleManager particleManager;
    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    @Shadow public abstract void updateWindowTitle();

    @Override
    public void stp$joinWorld(ClientWorld world) {
        ClientWorld previous = this.world;

        this.world = world;

        MinecraftClient client = (MinecraftClient) (Object) this;
        this.particleManager.setWorld(world);
        this.blockEntityRenderDispatcher.setWorld(world);
        this.updateWindowTitle();

        // ★ 关键修复：通知 AIT 等模组"世界已切换"
        // AIT 的 SoundHandler、ClientTardisUtil、和谐之眼渲染器等监听此事件，
        // 会清理旧维度的音效（塔迪斯嗡嗡声）和渲染状态（和谐之眼）。
        // 少了这一行 → 塔迪斯音效持续播放、和谐之眼跑到其他维度。
        try {
            ClientWorldEvents.CHANGE_WORLD.invoker().onChange(client, world);
        } catch (Throwable t) {
            STP.LOGGER.warn("Failed to fire ClientWorldEvents.CHANGE_WORLD", t);
        }

        // 单机时跳过认证服务初始化；连服务器时需要重建
        if (!this.integratedServerRunning) {
            try {
                ApiServices apiServices = ApiServices.create(this.authenticationService, this.runDirectory);
                apiServices.userCache().setExecutor(client);
                SkullBlockEntity.setServices(apiServices, client);
                UserCache.setUseRemote(false);
            } catch (Throwable t) {
                STP.LOGGER.warn("Failed to reinit ApiServices on joinWorld", t);
            }
        }
    }
}