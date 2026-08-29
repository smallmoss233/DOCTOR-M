package doctor_m.network;

import doctor_m.module.PlayerTitleManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * 称号网络同步
 */
public class TitleNetwork {

    public static final Identifier C2S_SET_TITLE = new Identifier("doctor_m", "set_title");
    public static final Identifier S2C_SYNC_TITLE = new Identifier("doctor_m", "sync_title");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_SET_TITLE, (server, player, handler, buf, responseSender) -> {
            String rawTitle = buf.readString(64);
            server.execute(() -> {
                String finalTitle = rawTitle.isBlank() ? null : rawTitle;
                PlayerTitleManager.setTitle(player, finalTitle);
                broadcastTitle(server, player.getUuid(), finalTitle);
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity joined = handler.getPlayer();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                String title = PlayerTitleManager.getTitle(p);
                sendTitleToPlayer(joined, p.getUuid(), title);
            }
        });
    }

    public static void broadcastTitle(net.minecraft.server.MinecraftServer server, UUID playerUuid, String title) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(playerUuid);
        buf.writeBoolean(title != null);
        if (title != null) buf.writeString(title);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, S2C_SYNC_TITLE, buf);
        }
    }

    public static void sendTitleToPlayer(ServerPlayerEntity target, UUID ownerUuid, String title) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(ownerUuid);
        buf.writeBoolean(title != null);
        if (title != null) buf.writeString(title);
        ServerPlayNetworking.send(target, S2C_SYNC_TITLE, buf);
    }
}