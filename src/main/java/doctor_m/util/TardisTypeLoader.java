package doctor_m.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TardisTypeLoader implements SimpleSynchronousResourceReloadListener {
    private static final Identifier ID = new Identifier("doctor_m", "tardis_type_loader");
    private static final Map<Identifier, String> MAP = new HashMap<>();

    public static void init() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new TardisTypeLoader());
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        Map<Identifier, String> newMap = new HashMap<>();

        // 直接限定在 doctor_m 命名空间下搜索，减少遍历
        Map<Identifier, Resource> resources = manager.findResources("doctor_m",
                path -> path.getPath().equals("tardis_type.json"));

        System.out.println("[TardisTypeLoader] Found " + resources.size() + " resource(s) in doctor_m namespace");

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            Resource resource = entry.getValue();

            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                for (Map.Entry<String, JsonElement> jsonEntry : json.entrySet()) {
                    String key = jsonEntry.getKey();
                    String value = jsonEntry.getValue().getAsString();
                    Identifier interiorId = new Identifier(key);
                    newMap.put(interiorId, value);
                }
            } catch (Exception e) {
                System.err.println("[TardisTypeLoader] Failed to load " + resourceId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        synchronized (MAP) {
            MAP.clear();
            MAP.putAll(newMap);
        }

        System.out.println("[TardisTypeLoader] Loaded " + MAP.size() + " interior type mappings.");
    }

    public static String getTypeForDesktop(Identifier desktopId) {
        synchronized (MAP) {
            return MAP.getOrDefault(desktopId, "Type 50 TT Capsule");
        }
    }
}