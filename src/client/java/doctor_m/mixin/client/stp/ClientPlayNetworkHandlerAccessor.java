package doctor_m.mixin.client.stp;

import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.CombinedDynamicRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPlayNetworkHandlerAccessor {

    @Accessor("combinedDynamicRegistries")
    CombinedDynamicRegistries<ClientDynamicRegistryType> getCombinedDynamicRegistries();

    @Accessor("world")
    ClientWorld getWorld();

    @Accessor("world")
    void setWorld(ClientWorld world);

    @Accessor("chunkLoadDistance")
    int getChunkLoadDistance();

    @Accessor("simulationDistance")
    int getSimulationDistance();

    @Accessor("worldProperties")
    ClientWorld.Properties getWorldProperties();

    @Accessor("worldProperties")
    void setWorldProperties(ClientWorld.Properties properties);
}