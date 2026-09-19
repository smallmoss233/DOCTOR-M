package mosslib.client.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class MossBedrock {

    public record CacheKey(Identifier id, String geometryId, float uvScale) {}

    private static final Map<CacheKey, MossBedrockModel> CACHE = new ConcurrentHashMap<>();

    private MossBedrock() {}

    public static MossBedrockModel parse(JsonObject json) {
        return parse(json, null, 1.0f);
    }

    public static MossBedrockModel parse(JsonObject json, String geometryId) {
        return parse(json, geometryId, 1.0f);
    }

    public static MossBedrockModel parse(JsonObject json, String geometryId, float uvScale) {
        MossGeometry model = MossGeometry.fromJson(json);
        return MossGeometryConverter.convert(model, geometryId, uvScale);
    }

    public static MossBedrockModel load(ResourceManager rm, Identifier id) {
        return load(rm, id, null, null);
    }

    public static MossBedrockModel load(ResourceManager rm, Identifier id, String geometryId) {
        return load(rm, id, geometryId, null);
    }

    public static MossBedrockModel load(ResourceManager rm, Identifier id,
                                        String geometryId, Identifier textureId) {
        float uvScale = resolveUvScale(rm, id, geometryId, textureId);
        CacheKey key = new CacheKey(id, geometryId, uvScale);
        return CACHE.computeIfAbsent(key, k -> readAndParse(rm, id, geometryId, uvScale));
    }

    private static float resolveUvScale(ResourceManager rm, Identifier modelId,
                                        String geometryId, Identifier textureId) {
        if (textureId == null) return 1.0f;
        int[] declared = MossTextureInfo.readDeclaredSize(rm, modelId, geometryId);
        int[] actual = MossTextureInfo.fromPng(rm, textureId);
        if (declared == null || actual == null) return 1.0f;
        if (declared[0] <= 0 || declared[1] <= 0) return 1.0f;
        if (actual[0] % declared[0] != 0 || actual[1] % declared[1] != 0) return 1.0f;
        float sx = (float) actual[0] / declared[0];
        float sy = (float) actual[1] / declared[1];
        if (Math.abs(sx - sy) > 1e-6f) return 1.0f;
        return sx;
    }

    private static MossBedrockModel readAndParse(ResourceManager rm, Identifier logicalId,
                                                 String geometryId, float uvScale) {
        Identifier fileId = new Identifier(
                logicalId.getNamespace(),
                "bedrock/" + logicalId.getPath() + ".geo.json");

        try {
            Resource resource = rm.getResource(fileId).orElseThrow(
                    () -> new MossGeometryException("Bedrock model not found: " + fileId));

            try (InputStreamReader reader = new InputStreamReader(
                    resource.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return parse(json, geometryId, uvScale);
            }
        } catch (MossGeometryException e) {
            throw e;
        } catch (Exception e) {
            throw new MossGeometryException("Failed to load bedrock model: " + fileId, e);
        }
    }

    public static void invalidate(Identifier id) {
        CACHE.keySet().removeIf(k -> k.id().equals(id));
    }

    public static void clearCache() {
        CACHE.clear();
    }
}