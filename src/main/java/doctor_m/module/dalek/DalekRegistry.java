package doctor_m.module.dalek;

import dev.amble.lib.register.datapack.SimpleDatapackRegistry;
import doctor_m.DOCTORM;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class DalekRegistry extends SimpleDatapackRegistry<Dalek> {

    private static final DalekRegistry instance = new DalekRegistry();

    /** 变体注册表。datapack 路径：data/doctor_m/entities/dalek/variants/ */
    public DalekRegistry() {
        super(Dalek::fromInputStream, Dalek.CODEC,
                "entities/dalek/variants", "entities/dalek/variants",
                true, DOCTORM.MOD_ID);
    }

    public static Dalek COMMANDER;
    public static Dalek CLASSIC;
    public static Dalek IMPERIAL;
    public static Dalek TIME_WAR;

    @Override
    protected void defaults() {
        COMMANDER = register(new Dalek(
                DOCTORM.id("dalek/commander"),
                DOCTORM.id("textures/entity/daleks/commander/commander_dalek.png"),
                DOCTORM.id("textures/entity/daleks/commander/commander_dalek_emission.png")
        ));

        CLASSIC = register(new Dalek(
                DOCTORM.id("dalek/classic"),
                DOCTORM.id("textures/entity/daleks/classic/classic_dalek.png"),
                DOCTORM.id("textures/entity/daleks/classic/classic_dalek_emission.png")
        ));

        IMPERIAL = register(new Dalek(
                DOCTORM.id("dalek/imperial"),
                DOCTORM.id("textures/entity/daleks/imperial/imperial_dalek.png"),
                DOCTORM.id("textures/entity/daleks/imperial/imperial_dalek_emission.png")
        ));

        TIME_WAR = register(new Dalek(
                DOCTORM.id("dalek/time_war"),
                DOCTORM.id("textures/entity/daleks/time_war/time_war_dalek.png"),
                DOCTORM.id("textures/entity/daleks/time_war/time_war_dalek_emission.png")
        ));
    }

    @Override
    public void onCommonInit() {
        super.onCommonInit();
        this.defaults();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this);
    }

    @Override
    public Dalek fallback() {
        return COMMANDER;
    }

    public static DalekRegistry getInstance() {
        return instance;
    }
}