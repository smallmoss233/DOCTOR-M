package doctor_m;

import dev.amble.lib.container.RegistryContainer;
import doctor_m.Item.item_group;
import doctor_m.Item.items;
import doctor_m.command.AITTardisBuilderCommand;
import doctor_m.config.ConfigManager;
import doctor_m.dimension.DimensionRegister;
import doctor_m.entities.Entities;
import doctor_m.module.creativity.creativity_items;
import doctor_m.module.space_plus.block.ModBlockEntities;
import doctor_m.module.space_plus.block.ModBlocks;
import doctor_m.module.space_plus.system.VacuumEatingHandler;
import doctor_m.network.DeMatGunNetwork;
import doctor_m.world_data.ShieldDamageHandler;
import doctor_m.world_data.TimeKey.GemDeathSaveHandler;
import doctor_m.world_data.TimeKey.GemTickHandler;
import doctor_m.world_data.TimeKey.PocketWatchFunction;
import doctor_m.world_data.TimeKey.TimeKeyFunction;
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

    @Override
    public void onInitialize() {
        RegistryContainer.register(items.class, MOD_ID);
        RegistryContainer.register(creativity_items.class, MOD_ID);
        DimensionRegister.register();
        ShieldDamageHandler.register();
        ModBlocks.register();
        ModBlockEntities.register();
        VacuumEatingHandler.register();

        TimeKeyFunction.INSTANCE.register();
        PocketWatchFunction.INSTANCE.register();
        GemDeathSaveHandler.INSTANCE.register();
        GemTickHandler.INSTANCE.register();

        items.registerAbilities();
        creativity_items.registerAbilities();
        item_group.registerItems();
        Entities.registerAttributes();
        DeMatGunNetwork.registerServerReceiver();

        CommandRegistrationCallback.EVENT.register(AITTardisBuilderCommand::register);
        ConfigManager.loadConfig();
    }
}