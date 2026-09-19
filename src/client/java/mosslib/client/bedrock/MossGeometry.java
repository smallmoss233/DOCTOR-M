package mosslib.client.bedrock;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

final class MossGeometry {

    static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Vec3.class, new Vec3Adapter())
            .registerTypeAdapter(UV.class, new UVAdapter())
            .create();

    @SerializedName("format_version")
    String formatVersion;

    @SerializedName("minecraft:geometry")
    List<Geometry> geometries;

    static MossGeometry fromJson(JsonObject json) {
        try {
            MossGeometry m = GSON.fromJson(json, MossGeometry.class);
            if (m == null) throw new MossGeometryException("Root JSON parsed to null");
            return m;
        } catch (JsonSyntaxException e) {
            throw new MossGeometryException("Malformed bedrock geometry JSON", e);
        }
    }

    // ─── 几何体 ───────────────────────────────────────

    static final class Geometry {
        Description description;
        List<Bone> bones;
    }

    static final class Description {
        String identifier;
        // ★ 用包装类型：缺省时为 null，避免 Gson 填 0 导致 TexturedModelData.of(0,0) 崩溃
        @SerializedName("texture_width")  Integer textureWidth;
        @SerializedName("texture_height") Integer textureHeight;
    }

    // ─── 骨骼 ─────────────────────────────────────────

    static final class Bone {
        String name;
        String parent;
        Vec3 pivot;
        Vec3 rotation;
        List<Cube> cubes;
        Map<String, Locator> locators;
    }

    static final class Locator {
        Vec3 offset;
        Vec3 rotation;
    }

    // ─── 立方体 ───────────────────────────────────────

    static final class Cube {
        Vec3 origin;
        Vec3 size;
        Vec3 pivot;
        Vec3 rotation;
        UV uv;
        float inflate;
        // ★ 用包装类型，防止 Blockbench 把 "true"/"false" 当字符串时静默失败
        @SerializedName("mirror")
        Boolean mirror;

        boolean isMirror() {
            return Boolean.TRUE.equals(mirror);
        }

        boolean hasRotation() {
            return rotation != null && !rotation.isZero();
        }
    }

    // ─── 向量 ─────────────────────────────────────────

    static final class Vec3 {
        static final Vec3 ZERO = new Vec3(0, 0, 0);
        final float x, y, z;

        Vec3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        boolean isZero() {
            return x == 0f && y == 0f && z == 0f;
        }
    }

    // ─── UV ───────────────────────────────────────────

    static final class UV {
        // ★ 改成 float[]：Box UV 可以是浮点，直接取整会导致累计 1~3 像素漂移
        float[] box;
        Face north, east, south, west, up, down;

        boolean isBox() {
            return box != null && box.length >= 2;
        }

        record Face(float[] uv, float[] uvSize) {}
    }

    // ─── Gson 适配器 ──────────────────────────────────

    private static final class Vec3Adapter implements JsonDeserializer<Vec3> {
        @Override
        public Vec3 deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            if (json == null || json.isJsonNull()) return Vec3.ZERO;
            JsonArray a = json.getAsJsonArray();
            float x = a.size() > 0 ? a.get(0).getAsFloat() : 0f;
            float y = a.size() > 1 ? a.get(1).getAsFloat() : 0f;
            float z = a.size() > 2 ? a.get(2).getAsFloat() : 0f;
            return new Vec3(x, y, z);
        }
    }

    private static final class UVAdapter implements JsonDeserializer<UV> {
        @Override
        public UV deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            UV uv = new UV();
            if (json.isJsonArray()) {
                JsonArray a = json.getAsJsonArray();
                uv.box = new float[a.size()];
                for (int i = 0; i < a.size(); i++) uv.box[i] = a.get(i).getAsFloat();
                return uv;
            }
            JsonObject o = json.getAsJsonObject();
            uv.north = face(o, "north");
            uv.east  = face(o, "east");
            uv.south = face(o, "south");
            uv.west  = face(o, "west");
            uv.up    = face(o, "up");
            uv.down  = face(o, "down");
            return uv;
        }

        private static UV.Face face(JsonObject o, String key) {
            if (!o.has(key)) return null;
            JsonObject f = o.getAsJsonObject(key);
            return new UV.Face(readFloats(f.getAsJsonArray("uv")),
                    readFloats(f.getAsJsonArray("uv_size")));
        }

        private static float[] readFloats(JsonArray arr) {
            if (arr == null) return new float[0];
            float[] out = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) out[i] = arr.get(i).getAsFloat();
            return out;
        }
    }
}