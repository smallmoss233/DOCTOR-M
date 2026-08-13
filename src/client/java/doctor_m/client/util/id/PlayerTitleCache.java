package doctor_m.client.util.id;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端称号缓存
 * 所有玩家称号存在这里，渲染时直接读
 */
public final class PlayerTitleCache {

    public static final Identifier S2C_SYNC_TITLE = new Identifier("doctor_m", "sync_title");

    private static final Map<UUID, String> CACHE = new HashMap<>();

    private PlayerTitleCache() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(S2C_SYNC_TITLE, (client, handler, buf, responseSender) -> {
            UUID uuid = buf.readUuid();
            boolean hasTitle = buf.readBoolean();
            String title = hasTitle ? buf.readString() : null;

            client.execute(() -> {
                if (title == null || title.isBlank()) {
                    CACHE.remove(uuid);
                } else {
                    CACHE.put(uuid, title);
                }
            });
        });
    }

    public static String getTitle(UUID uuid) {
        return CACHE.get(uuid);
    }

    public static void clear() {
        CACHE.clear();
    }
}