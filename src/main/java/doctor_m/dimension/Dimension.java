package doctor_m.dimension;

import dev.drtheo.multidim.MultiDim;
import dev.drtheo.multidim.api.WorldBlueprint;
import doctor_m.DOCTORM;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

public class Dimension implements ModInitializer {
    public static WorldBlueprint TRENZALORE;
    public static WorldBlueprint TITAN;

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {

            // 特兰泽洛
            TRENZALORE = new WorldBlueprint(new Identifier(DOCTORM.MOD_ID, "trenzalore"))
                    .setPersistent(true)
                    .shouldTickTime(false)
                    .setAutoLoad(true)
                    .withType(new Identifier(DOCTORM.MOD_ID, "trenzalore_dimension_type"));

            MultiDim.get(server).register(TRENZALORE);

            // 泰坦
            TITAN = new WorldBlueprint(new Identifier(DOCTORM.MOD_ID, "titan"))
                    .setPersistent(true)
                    .shouldTickTime(false)
                    .setAutoLoad(true)
                    .withType(new Identifier(DOCTORM.MOD_ID, "titan"));

            MultiDim.get(server).register(TITAN);
        });
    }
}