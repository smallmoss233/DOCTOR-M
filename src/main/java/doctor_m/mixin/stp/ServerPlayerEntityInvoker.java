package doctor_m.mixin.stp;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerEntityInvoker {

    @Invoker("worldChanged")
    void stp$worldChanged(ServerWorld world);

    @Accessor("syncedExperience")
    void setSyncedExperience(int xp);

    @Accessor("syncedHealth")
    void setSyncedHealth(float health);

    @Accessor("syncedFoodLevel")
    void setSyncedFoodLevel(int foodLevel);

    @Accessor("inTeleportationState")
    void setInTeleportationState(boolean inTeleportationState);

    @Invoker("getTeleportTarget")
    TeleportTarget stp$getTeleportTarget(ServerWorld world);
}