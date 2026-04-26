package doctor_m.dimension;

import dev.drtheo.multidim.MultiDim;
import dev.drtheo.multidim.api.WorldBlueprint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

public class trenzalore implements ModInitializer {
    public static WorldBlueprint TRENZALORE;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            TRENZALORE = new WorldBlueprint(new Identifier("doctor_m", "trenzalore"))
                    .setPersistent(true)
                    .shouldTickTime(false)
                    .setAutoLoad(true);

            MultiDim.get(server).register(TRENZALORE);
        });
    }
    public static void register() {
    }
}