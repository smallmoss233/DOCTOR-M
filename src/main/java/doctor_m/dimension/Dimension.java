package doctor_m.dimension;

import dev.drtheo.multidim.MultiDim;
import dev.drtheo.multidim.api.WorldBlueprint;
import doctor_m.DOCTORM;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class Dimension implements ModInitializer {
    public static WorldBlueprint TRENZALORE;
    public static WorldBlueprint TITAN;

    @Override
    public void onInitialize() {
        // 先注册生成器
        DimensionRegister.register();

        // 特兰泽洛
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            TRENZALORE = new WorldBlueprint(new Identifier("doctor_m", "trenzalore"))
                    .setPersistent(true)
                    .shouldTickTime(false)
                    .setAutoLoad(true)
                    .withType(new Identifier("doctor_m", "trenzalore_dimension_type"));

            MultiDim.get(server).register(TRENZALORE);

            // 泰坦 - withType 接受 Identifier
            TITAN = new WorldBlueprint(new Identifier(DOCTORM.MOD_ID, "titan"))
                    .setPersistent(true)
                    .shouldTickTime(false)
                    .setAutoLoad(true)
                    .withType(new Identifier(DOCTORM.MOD_ID, "titan")); // 使用 Identifier

            MultiDim.get(server).register(TITAN);
        });
    }

    // 土卫六-泰坦
    public static final RegistryKey<DimensionType> TITAN_DIMENSION_TYPE = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(DOCTORM.MOD_ID, "titan")
    );

    public static final RegistryKey<World> TITAN_WORLD = RegistryKey.of(
            RegistryKeys.WORLD,
            new Identifier(DOCTORM.MOD_ID, "titan")
    );
}