package mosslib.client.bedrock.block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
            MossGeometry geometry = GSON.fromJson(json, MossGeometry.class);
            if (geometry == null) {
                throw new MossGeometryException("Root JSON parsed to null");
            }
            return geometry;
        } catch (JsonSyntaxException e) {
            throw new MossGeometryException("Malformed bedrock geometry JSON", e);
        }
    }

    static final class Geometry {
        Description description;
        List<Bone> bones;
    }

    static final class Description {
        String identifier;

        // 包装类型：缺失时保持 null，避免被 Gson 填成 0。
        @SerializedName("texture_width")
        Integer textureWidth;

        @SerializedName("texture_height")
        Integer textureHeight;
    }

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

    static final class Cube {
        Vec3 origin;
        Vec3 size;
        Vec3 pivot;
        Vec3 rotation;
        UV uv;
        float inflate;

        // 包装类型：Blockbench 有时把 mirror 写成 "true" / "false" 字符串，
        // 用 Boolean 可以让缺省与显式 false 区分开。
        Boolean mirror;

        boolean isMirror() {
            return Boolean.TRUE.equals(mirror);
        }

        boolean hasRotation() {
            return rotation != null && !rotation.isZero();
        }
    }

    static final class Vec3 {
        static final Vec3 ZERO = new Vec3(0f, 0f, 0f);

        final float x;
        final float y;
        final float z;

        Vec3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        boolean isZero() {
            return x == 0f && y == 0f && z == 0f;
        }
    }

    static final class UV {
        /**
         * Box UV，存为 {@code [u, v, width, height]} 浮点数组。
         * 使用 float 而非 int：Blockbench 支持亚像素 UV，取整会导致累计漂移。
         */
        float[] box;

        Face north;
        Face east;
        Face south;
        Face west;
        Face up;
        Face down;

        boolean isBox() {
            return box != null && box.length >= 2;
        }

        record Face(float[] uv, float[] uvSize) {}
    }

    /** 接受 {@code [x, y, z]} 数组、缺失字段或显式 null（→ {@link Vec3#ZERO}）。 */
    private static final class Vec3Adapter implements JsonDeserializer<Vec3> {
        @Override
        public Vec3 deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            if (json == null || json.isJsonNull()) {
                return Vec3.ZERO;
            }
            JsonArray array = json.getAsJsonArray();
            return new Vec3(
                    readFloat(array, 0),
                    readFloat(array, 1),
                    readFloat(array, 2));
        }
    }

    private static final class UVAdapter implements JsonDeserializer<UV> {
        @Override
        public UV deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
            UV uv = new UV();

            if (json.isJsonArray()) {
                uv.box = readFloats(json.getAsJsonArray());
                return uv;
            }

            JsonObject obj = json.getAsJsonObject();
            uv.north = readFace(obj, "north");
            uv.east  = readFace(obj, "east");
            uv.south = readFace(obj, "south");
            uv.west  = readFace(obj, "west");
            uv.up    = readFace(obj, "up");
            uv.down  = readFace(obj, "down");
            return uv;
        }

        private static UV.Face readFace(JsonObject parent, String key) {
            JsonElement element = parent.get(key);
            if (element == null || element.isJsonNull()) {
                return null;
            }
            JsonObject face = element.getAsJsonObject();
            return new UV.Face(
                    readFloats(face.getAsJsonArray("uv")),
                    readFloats(face.getAsJsonArray("uv_size")));
        }
    }

    private static float readFloat(JsonArray array, int index) {
        if (array == null || index >= array.size()) {
            return 0f;
        }
        JsonElement element = array.get(index);
        return element == null || element.isJsonNull() ? 0f : element.getAsFloat();
    }

    private static float[] readFloats(JsonArray array) {
        if (array == null) {
            return new float[0];
        }
        int size = array.size();
        float[] out = new float[size];
        for (int i = 0; i < size; i++) {
            JsonElement element = array.get(i);
            out[i] = (element == null || element.isJsonNull()) ? 0f : element.getAsFloat();
        }
        return out;
    }
}