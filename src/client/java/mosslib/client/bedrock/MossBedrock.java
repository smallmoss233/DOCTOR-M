package mosslib.client.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blockbench 基岩版模型加载器。
 *
 * <p>与 AmbleKit 的 {@code BedrockModel} 不同：
 * <ul>
 *   <li>支持一个文件里多个 geometry，按 identifier 选择</li>
 *   <li>内部用 float 而不是 List&lt;Float&gt;，避免装箱开销</li>
 *   <li>支持单个立方体独立旋转</li>
 *   <li>异常带骨骼名/立方体位置信息</li>
 *   <li>按 Identifier 缓存结果</li>
 * </ul>
 *
 * <p>已知不支持：per-face UV（会抛异常），未来可扩展。
 */
@Environment(EnvType.CLIENT)
public final class MossBedrock {

    private static final Map<Identifier, TexturedModelData> CACHE = new ConcurrentHashMap<>();

    private MossBedrock() {}

    // ─── 直接解析 JsonObject ─────────────────────────

    public static TexturedModelData parse(JsonObject json) {
        return parse(json, null);
    }

    public static TexturedModelData parse(JsonObject json, String geometryId) {
        MossGeometry model = MossGeometry.fromJson(json);
        return MossGeometryConverter.convert(model, geometryId);
    }

    // ─── 从资源管理器加载（带缓存） ──────────────────

    public static TexturedModelData load(ResourceManager rm, Identifier id) {
        return load(rm, id, null);
    }

    public static TexturedModelData load(ResourceManager rm, Identifier id, String geometryId) {
        Identifier key = geometryId == null
                ? id
                : new Identifier(id.getNamespace(), id.getPath() + "#" + geometryId);

        return CACHE.computeIfAbsent(key, k -> readAndParse(rm, id, geometryId));
    }

    private static TexturedModelData readAndParse(ResourceManager rm, Identifier logicalId, String geometryId) {
        // 逻辑 ID: doctor_m:coffee_machine
        // → 实际文件: assets/doctor_m/bedrock/coffee_machine.geo.json
        Identifier fileId = new Identifier(
                logicalId.getNamespace(),
                "bedrock/" + logicalId.getPath() + ".geo.json"
        );

        try {
            Resource resource = rm.getResource(fileId).orElseThrow(
                    () -> new MossGeometryException("Bedrock model not found: " + fileId));

            try (InputStreamReader reader = new InputStreamReader(
                    resource.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return parse(json, geometryId);
            }
        } catch (MossGeometryException e) {
            throw e;
        } catch (Exception e) {
            throw new MossGeometryException("Failed to load bedrock model: " + fileId, e);
        }
    }

    // ─── 缓存管理 ────────────────────────────────────

    public static void invalidate(Identifier id) {
        CACHE.remove(id);
    }

    public static void clearCache() {
        CACHE.clear();
    }
}