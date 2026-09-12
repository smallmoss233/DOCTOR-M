package doctor_m.mixin.client.stp;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import doctor_m.client.util.stp.STPMinecraftClient;
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
        this.world = world;

        MinecraftClient client = (MinecraftClient) (Object) this;
        this.particleManager.setWorld(world);
        this.blockEntityRenderDispatcher.setWorld(world);
        this.updateWindowTitle();

        // 非集成服务器（真实服务器）场景下，重新初始化认证服务
        // 用 try-catch 包裹，避免网络异常影响传送
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