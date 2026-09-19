package mosslib.bedrockbolck;

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

    private static final String BEDROCK_DIRECTORY = "bedrock/";
    private static final String GEO_SUFFIX = ".geo.json";
    private static final float DEFAULT_UV_SCALE = 1.0f;
    private static final float UV_SCALE_EPSILON = 1.0e-6f;

    private record ModelCacheKey(Identifier id, String geometryId, float uvScale) {}

    private record UvScaleCacheKey(Identifier modelId, String geometryId, Identifier textureId) {}

    private static final Map<ModelCacheKey, MossBedrockModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<UvScaleCacheKey, Float> UV_SCALE_CACHE = new ConcurrentHashMap<>();

    private MossBedrock() {}

    public static MossBedrockModel parse(JsonObject json) {
        return parse(json, null, DEFAULT_UV_SCALE);
    }

    public static MossBedrockModel parse(JsonObject json, String geometryId) {
        return parse(json, geometryId, DEFAULT_UV_SCALE);
    }

    public static MossBedrockModel parse(JsonObject json, String geometryId, float uvScale) {
        MossGeometry geometry = MossGeometry.fromJson(json);
        return MossGeometryConverter.convert(geometry, geometryId, uvScale);
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
        ModelCacheKey key = new ModelCacheKey(id, geometryId, uvScale);
        return MODEL_CACHE.computeIfAbsent(key, k -> readAndParse(rm, id, geometryId, uvScale));
    }

    private static float resolveUvScale(ResourceManager rm, Identifier modelId,
                                        String geometryId, Identifier textureId) {
        if (textureId == null) {
            return DEFAULT_UV_SCALE;
        }

        UvScaleCacheKey key = new UvScaleCacheKey(modelId, geometryId, textureId);
        return UV_SCALE_CACHE.computeIfAbsent(key,
                k -> computeUvScale(rm, modelId, geometryId, textureId));
    }

    private static float computeUvScale(ResourceManager rm, Identifier modelId,
                                        String geometryId, Identifier textureId) {
        int[] declared = MossTextureInfo.readDeclaredSize(rm, modelId, geometryId);
        int[] actual = MossTextureInfo.fromPng(rm, textureId);

        if (declared == null || actual == null) {
            return DEFAULT_UV_SCALE;
        }
        if (declared[0] <= 0 || declared[1] <= 0) {
            return DEFAULT_UV_SCALE;
        }
        if (actual[0] % declared[0] != 0 || actual[1] % declared[1] != 0) {
            return DEFAULT_UV_SCALE;
        }

        float sx = (float) actual[0] / declared[0];
        float sy = (float) actual[1] / declared[1];
        if (Math.abs(sx - sy) > UV_SCALE_EPSILON) {
            return DEFAULT_UV_SCALE;
        }
        return sx;
    }

    private static MossBedrockModel readAndParse(ResourceManager rm, Identifier logicalId,
                                                 String geometryId, float uvScale) {
        Identifier fileId = new Identifier(
                logicalId.getNamespace(),
                BEDROCK_DIRECTORY + logicalId.getPath() + GEO_SUFFIX);

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
        MODEL_CACHE.keySet().removeIf(k -> k.id().equals(id));
        UV_SCALE_CACHE.keySet().removeIf(k ->
                k.modelId().equals(id) || k.textureId().equals(id));
    }

    public static void clearCache() {
        MODEL_CACHE.clear();
        UV_SCALE_CACHE.clear();
    }
}