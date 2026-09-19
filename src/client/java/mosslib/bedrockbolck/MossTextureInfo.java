package mosslib.bedrockbolck;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 从 geo.json 读声明尺寸，从 PNG 头读实际尺寸。
 * 用于自动推断 UV 缩放，以及在尺寸不一致时打警告。
 */
@Environment(EnvType.CLIENT)
public final class MossTextureInfo {

    private MossTextureInfo() {}

    /** 从 geo.json 里读 texture_width/height；未声明或读失败返回 null。 */
    public static int[] readDeclaredSize(ResourceManager rm, Identifier modelId, String geometryId) {
        Identifier fileId = new Identifier(
                modelId.getNamespace(),
                "bedrock/" + modelId.getPath() + ".geo.json");
        try {
            Resource res = rm.getResource(fileId).orElse(null);
            if (res == null) return null;
            try (InputStream in = res.getInputStream()) {
                String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(s).getAsJsonObject();
                return fromGeoJson(root, geometryId);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static int[] fromGeoJson(JsonObject root, String geometryId) {
        var geos = root.getAsJsonArray("minecraft:geometry");
        if (geos == null || geos.isEmpty()) return null;

        JsonObject chosen = null;
        if (geometryId == null || geometryId.isEmpty()) {
            chosen = geos.get(0).getAsJsonObject();
        } else {
            for (JsonElement e : geos) {
                JsonObject g = e.getAsJsonObject();
                JsonObject d = g.getAsJsonObject("description");
                if (d != null && d.has("identifier")
                        && geometryId.equals(d.get("identifier").getAsString())) {
                    chosen = g;
                    break;
                }
            }
        }
        if (chosen == null) return null;

        JsonObject desc = chosen.getAsJsonObject("description");
        if (desc == null) return null;

        int w = desc.has("texture_width")  ? desc.get("texture_width").getAsInt()  : 0;
        int h = desc.has("texture_height") ? desc.get("texture_height").getAsInt() : 0;
        if (w <= 0 || h <= 0) return null;
        return new int[]{ w, h };
    }

    /** 只读 PNG 头 24 字节拿 IHDR 宽高。不是 PNG 或读失败返回 null。 */
    public static int[] fromPng(ResourceManager rm, Identifier pngId) {
        try {
            Resource res = rm.getResource(pngId).orElse(null);
            if (res == null) return null;
            try (InputStream in = res.getInputStream()) {
                byte[] h = in.readNBytes(24);
                if (h.length < 24) return null;
                // PNG 签名 89 50 4E 47 0D 0A 1A 0A
                if ((h[0] & 0xFF) != 0x89 || h[1] != 'P'
                        || h[2] != 'N' || h[3] != 'G') return null;
                int w = ((h[16] & 0xFF) << 24) | ((h[17] & 0xFF) << 16)
                        | ((h[18] & 0xFF) << 8)  |  (h[19] & 0xFF);
                int hh = ((h[20] & 0xFF) << 24) | ((h[21] & 0xFF) << 16)
                        | ((h[22] & 0xFF) << 8)  |  (h[23] & 0xFF);
                return new int[]{ w, hh };
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** 第一次碰到某模型时调一次，打日志。 */
    public static void diagnose(ResourceManager rm, Identifier modelId, Identifier pngId) {
        int[] declared = readDeclaredSize(rm, modelId, null);
        int[] actual = fromPng(rm, pngId);

        String dStr = declared == null ? "?" : declared[0] + "x" + declared[1];
        String aStr = actual   == null ? "?" : actual[0]   + "x" + actual[1];

        System.out.println("[MossBedrock] model=" + modelId
                + " texture=" + pngId
                + " declared=" + dStr
                + " actualPNG=" + aStr);

        if (declared != null && actual != null
                && (declared[0] != actual[0] || declared[1] != actual[1])) {
            System.err.println("[MossBedrock] ⚠ size mismatch; auto-scaling if ratio is integer. "
                    + "declared=" + dStr + " actual=" + aStr);
        }
    }
}