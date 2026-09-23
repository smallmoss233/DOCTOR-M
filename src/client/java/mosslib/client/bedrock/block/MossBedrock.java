package mosslib.client.bedrock.block;

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

    /** 默认模式：方块（根骨骼 pivot 抹成 0）。 */
    public static final boolean BLOCK_MODE = false;
    /** 实体模式：根骨骼使用真实 pivot。 */
    public static final boolean ENTITY_MODE = true;

    private record ModelCacheKey(Identifier id, String geometryId,
                                 float uvScale, boolean applyRootPivot) {}

    private record UvScaleCacheKey(Identifier modelId, String geometryId, Identifier textureId) {}

    private static final Map<ModelCacheKey, MossBedrockModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<UvScaleCacheKey, Float> UV_SCALE_CACHE = new ConcurrentHashMap<>();

    private MossBedrock() {}

    // =====================================================================
    // parse
    // =====================================================================

    public static MossBedrockModel parse(JsonObject json) {
        return parse(json, null, DEFAULT_UV_SCALE, BLOCK_MODE);
    }

    public static MossBedrockModel parse(JsonObject json, String geometryId) {
        return parse(json, geometryId, DEFAULT_UV_SCALE, BLOCK_MODE);
    }

    public static MossBedrockModel parse(JsonObject json, String geometryId, float uvScale) {
        return parse(json, geometryId, uvScale, BLOCK_MODE);
    }

    public static MossBedrockModel parse(JsonObject json, String geometryId,
                                         float uvScale, boolean applyRootPivot) {
        MossGeometry geometry = MossGeometry.fromJson(json);
        return MossGeometryConverter.convert(geometry, geometryId, uvScale, applyRootPivot);
    }

    // =====================================================================
    // load —— 默认方块模式
    // =====================================================================

    public static MossBedrockModel load(ResourceManager rm, Identifier id) {
        return load(rm, id, null, null, BLOCK_MODE);
    }

    public static MossBedrockModel load(ResourceManager rm, Identifier id, String geometryId) {
        return load(rm, id, geometryId, null, BLOCK_MODE);
    }

    public static MossBedrockModel load(ResourceManager rm, Identifier id,
                                        String geometryId, Identifier textureId) {
        return load(rm, id, geometryId, textureId, BLOCK_MODE);
    }

    // =====================================================================
    // load —— 完整版，带 applyRootPivot
    // =====================================================================

    public static MossBedrockModel load(ResourceManager rm, Identifier id,
                                        String geometryId, Identifier textureId,
                                        boolean applyRootPivot) {
        float uvScale = resolveUvScale(rm, id, geometryId, textureId);
        ModelCacheKey key = new ModelCacheKey(id, geometryId, uvScale, applyRootPivot);
        return MODEL_CACHE.computeIfAbsent(key,
                k -> readAndParse(rm, id, geometryId, uvScale, applyRootPivot));
    }

    // =====================================================================
    // 内部
    // =====================================================================

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
                                                 String geometryId, float uvScale,
                                                 boolean applyRootPivot) {
        Identifier fileId = new Identifier(
                logicalId.getNamespace(),
                BEDROCK_DIRECTORY + logicalId.getPath() + GEO_SUFFIX);

        try {
            Resource resource = rm.getResource(fileId).orElseThrow(
                    () -> new MossGeometryException("Bedrock model not found: " + fileId));

            try (InputStreamReader reader = new InputStreamReader(
                    resource.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return parse(json, geometryId, uvScale, applyRootPivot);
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