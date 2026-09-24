package doctor_m;

import doctor_m.Item.item_group;
import doctor_m.Item.items;
import mosslib.api.AutoRegister;
import doctor_m.api.ModSounds;
import doctor_m.block.ModBlockEntities;
import doctor_m.block.ModBlocks;
import doctor_m.command.DOCTORMCommand;
import doctor_m.config.ConfigManager;
import doctor_m.entities.Entities;
import doctor_m.handler.KeytoTime.GemDeathSaveHandler;
import doctor_m.handler.KeytoTime.GemTickHandler;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.handler.KeytoTime.PocketWatchFunction;
import doctor_m.handler.ShieldDamageHandler;
import doctor_m.handler.VMServerHandler;
import doctor_m.module.STP;
import doctor_m.module.creativity.CreativityItems;
import doctor_m.module.creativity.creativity_data.Tlipoca.TlipocaScytheEvents;
import doctor_m.module.space_plus.VacuumEatingHandler;
import doctor_m.network.*;
import doctor_m.util.type.TardisTypeLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;

public class DOCTORM implements ModInitializer {

    public static final String MOD_ID = "doctor_m";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        //方块 / 方块实体 / 实体
        ModBlocks.register();
        ModBlockEntities.register();
        Entities.register();

        //物品
        AutoRegister.items(items.class, MOD_ID);
        AutoRegister.items(CreativityItems.class, MOD_ID);
        item_group.registerItems();

        //网络通道
        KeytoTimeTeleportNetwork.register();
        KeytoTimeNetwork.register();
        KeytoTimeActiveNetwork.register();
        DeMatGunNetwork.registerServerReceiver();
        TitleNetwork.register();
        UpdateObeliskPacket.registerServerReceiver();
        SARNetworking.register();

        //事件与处理器
        ShieldDamageHandler.register();
        VacuumEatingHandler.register();
        VMServerHandler.register();
        TlipocaScytheEvents.register();
        KeytoTimeCore.register();
        PocketWatchFunction.register();
        GemDeathSaveHandler.register();
        GemTickHandler.register();

        //杂项
        ModSounds.init();
        TardisTypeLoader.init();

        //命令与配置
        CommandRegistrationCallback.EVENT.register(DOCTORMCommand::register);
        ConfigManager.loadConfig();

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            STP.onPlayerDisconnect(handler.getPlayer());
        });
    }
}