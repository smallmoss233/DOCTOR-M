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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import world_data.GemDeathSaveHandler;
import world_data.GemTickHandler;
import world_data.PocketWatchFunction;
import world_data.TimeKeyFunction;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class DOCTORM implements ModInitializer {
    public static final String MOD_ID = "doctor_m";
    public static final Identifier TOGGLE_PASSIVE_A = new Identifier(MOD_ID, "toggle_passive_a");
    public static final Identifier TOGGLE_PASSIVE_B = new Identifier(MOD_ID, "toggle_passive_b");

    @Override
    public void onInitialize() {
        RegistryContainer.register(items.class, MOD_ID);
        RegistryContainer.register(creativity_items.class, MOD_ID);
        items.registerAbilities();
        creativity_items.registerAbilities();
        item_group.registerItems();
        entities.registerAttributes();
        dimension_register.register();
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