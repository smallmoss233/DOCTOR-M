package doctor_m;

import dev.amble.lib.container.RegistryContainer;
import doctor_m.Item.item_group;
import doctor_m.Item.items;
import doctor_m.command.AITTardisBuilderCommand;
import doctor_m.config.ConfigManager;
import doctor_m.dimension.DimensionRegister;
import doctor_m.entities.Entities;
import doctor_m.handler.ShieldDamageHandler;
import doctor_m.handler.TimeKey.GemDeathSaveHandler;
import doctor_m.handler.TimeKey.GemTickHandler;
import doctor_m.handler.TimeKey.PocketWatchFunction;
import doctor_m.handler.TimeKey.TimeKeyFunction;
import doctor_m.handler.VMServerHandler;
import doctor_m.module.creativity.CreativityItems;
import doctor_m.module.creativity.creativity_data.TlipocaScytheEvents;
import doctor_m.module.space_plus.block.ModBlockEntities;
import doctor_m.module.space_plus.block.ModBlocks;
import doctor_m.module.space_plus.system.VacuumEatingHandler;
import doctor_m.network.DeMatGunNetwork;
import doctor_m.network.TimeKeyActiveNetwork;
import doctor_m.network.TimeKeyNetwork;
import doctor_m.network.TimeKeyTeleportNetwork;
import doctor_m.util.type.TardisTypeLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";
    public static final Identifier TOGGLE_PASSIVE_A = new Identifier(MOD_ID, "toggle_passive_a");
    public static final Identifier TOGGLE_PASSIVE_B = new Identifier(MOD_ID, "toggle_passive_b");
    public static final SoundEvent SHIELD_ACTIVATE = register("shieldcore");
    public static final SoundEvent DE_MAT_GUN_FIRE = register("item.de_mat_gun.fire");
    public static final SoundEvent DE_MAT_GUN_ERASE = register("entity.de_mat_gun.erase");

    private static SoundEvent register(String name) {
        Identifier id = new Identifier(DOCTORM.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        RegistryContainer.register(items.class, MOD_ID);
        RegistryContainer.register(CreativityItems.class, MOD_ID);
        DimensionRegister.register();
        ShieldDamageHandler.register();
        ModBlocks.register();
        ModBlockEntities.register();
        VacuumEatingHandler.register();
        VMServerHandler.register();
        TlipocaScytheEvents.register();

        TimeKeyTeleportNetwork.register();
        TimeKeyNetwork.register();
        TimeKeyActiveNetwork.register();
        TimeKeyFunction.INSTANCE.register();
        PocketWatchFunction.INSTANCE.register();
        GemDeathSaveHandler.INSTANCE.register();
        GemTickHandler.INSTANCE.register();
        TardisTypeLoader.init();

        items.registerAbilities();
        CreativityItems.registerAbilities();
        item_group.registerItems();
        Entities.registerAttributes();
        DeMatGunNetwork.registerServerReceiver();

        CommandRegistrationCallback.EVENT.register(AITTardisBuilderCommand::register);
        ConfigManager.loadConfig();
    }
}