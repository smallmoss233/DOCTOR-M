package doctor_m.client.util.id;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerTitleCache {

    public static final Identifier S2C_SYNC_TITLE = new Identifier("doctor_m", "sync_title");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> CACHE = new HashMap<>();

    private PlayerTitleCache() {}

    public static void register() {
        load();

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
                save();
            });
        });
    }

    public static String getTitle(UUID uuid) {
        return CACHE.get(uuid);
    }

    public static void clear() {
        CACHE.clear();
        save();
    }

    private static Path getSavePath() {
        return MinecraftClient.getInstance().runDirectory
                .toPath()
                .resolve("config/doctor_m/player_titles.json");
    }

    private static void load() {
        Path path = getSavePath();
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path)) {
            Map<String, String> raw = GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (raw != null) {
                CACHE.clear();
                raw.forEach((k, v) -> CACHE.put(UUID.fromString(k), v));
            }
        } catch (IOException e) {
            System.err.println("[DoctorM] 加载称号缓存失败: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            Path path = getSavePath();
            Files.createDirectories(path.getParent());

            Map<String, String> raw = new HashMap<>();
            CACHE.forEach((k, v) -> raw.put(k.toString(), v));

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(raw, writer);
            }
        } catch (IOException e) {
            System.err.println("[DoctorM] 保存称号缓存失败: " + e.getMessage());
        }
    }
}