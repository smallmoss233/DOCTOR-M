package doctor_m;

import dev.amble.lib.container.RegistryContainer;
import doctor_m.Item.item_group;
import doctor_m.dimension.dimension_register;
import doctor_m.entities.entities;
import doctor_m.Item.items;
import doctor_m.module.ait_space_mixin.ModBlockEntities;
import doctor_m.module.ait_space_mixin.ModBlocks;
import doctor_m.module.creativity.creativity_items;
import doctor_m.util.command.AITTardisBuilderCommand;
import doctor_m.util.config.ConfigManager;
import doctor_m.world_data.Shield.ShieldDamageHandler;
import doctor_m.world_data.TimeKey.GemDeathSaveHandler;
import doctor_m.world_data.TimeKey.GemTickHandler;
import doctor_m.world_data.TimeKey.PocketWatchFunction;
import doctor_m.world_data.TimeKey.TimeKeyFunction;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";
    public static final Identifier TOGGLE_PASSIVE_A = new Identifier(MOD_ID, "toggle_passive_a");
    public static final Identifier TOGGLE_PASSIVE_B = new Identifier(MOD_ID, "toggle_passive_b");
    public static final SoundEvent SHIELD_ACTIVATE = register("shieldcore");

    private static SoundEvent register(String name) {
        Identifier id = new Identifier(DOCTORM.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    @Override
    public void onInitialize() {
        RegistryContainer.register(items.class, MOD_ID);
        RegistryContainer.register(creativity_items.class, MOD_ID);
        items.registerAbilities();
        creativity_items.registerAbilities();
        item_group.registerItems();
        entities.registerAttributes();
        dimension_register.register();
        ShieldDamageHandler.register();
        TimeKeyFunction.INSTANCE.register();
        PocketWatchFunction.INSTANCE.register();
        GemDeathSaveHandler.INSTANCE.register();
        GemTickHandler.INSTANCE.register();
        ModBlocks.register();
        ModBlockEntities.register();
        CommandRegistrationCallback.EVENT.register(AITTardisBuilderCommand::register);
        ConfigManager.loadConfig();
    }
}