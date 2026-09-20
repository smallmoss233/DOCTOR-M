package mosslib.client.bedrock.block;

import mosslib.client.bedrock.block.MossGeometry.*;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;

import java.util.*;

@Environment(EnvType.CLIENT)
final class MossGeometryConverter {

    private static final int DEFAULT_TEX_W = 64;
    private static final int DEFAULT_TEX_H = 64;

    private MossGeometryConverter() {}

    static MossBedrockModel convert(MossGeometry model, String wantedGeometry, float uvScale) {
        if (model.geometries == null || model.geometries.isEmpty()) {
            throw new MossGeometryException("Model has no geometry entries");
        }

        Geometry geometry = selectGeometry(model.geometries, wantedGeometry);
        Description desc = geometry.description;
        if (desc == null) {
            throw new MossGeometryException("Selected geometry has no description block");
        }
        if (geometry.bones == null || geometry.bones.isEmpty()) {
            throw new MossGeometryException("Geometry '" + desc.identifier + "' has no bones");
        }

        ModelData data = new ModelData();
        Map<String, Bone> boneIndex = indexBones(geometry.bones);
        Map<String, ModelPartData> built = new HashMap<>();
        List<MossPerFaceCube> deferred = new ArrayList<>();

        buildBones(geometry.bones, boneIndex, built, data, uvScale, deferred);

        int texW = desc.textureWidth  != null ? desc.textureWidth  : DEFAULT_TEX_W;
        int texH = desc.textureHeight != null ? desc.textureHeight : DEFAULT_TEX_H;
        int scaledW = Math.round(texW * uvScale);
        int scaledH = Math.round(texH * uvScale);

        return new MossBedrockModel(
                TexturedModelData.of(data, scaledW, scaledH),
                deferred,
                scaledW,
                scaledH);
    }

    private static Geometry selectGeometry(List<Geometry> all, String wanted) {
        if (wanted == null || wanted.isEmpty()) return all.get(0);
        for (Geometry g : all) {
            if (g.description != null && wanted.equals(g.description.identifier)) return g;
        }
        throw new MossGeometryException("Geometry '" + wanted + "' not found in file");
    }

    private static Map<String, Bone> indexBones(List<Bone> bones) {
        Map<String, Bone> map = new HashMap<>(bones.size());
        for (Bone b : bones) {
            if (b.name == null || b.name.isEmpty()) {
                throw new MossGeometryException("Found bone with empty name");
            }
            map.put(b.name, b);
        }
        return map;
    }

    // ─── 骨骼遍历 ────────────────────────────────────

    private static void buildBones(List<Bone> bones, Map<String, Bone> index,
                                   Map<String, ModelPartData> built, ModelData data,
                                   float uvScale, List<MossPerFaceCube> deferred) {
        List<Bone> pending = new ArrayList<>(bones);
        int guard = pending.size() + 1;

        while (!pending.isEmpty() && guard-- > 0) {
            Iterator<Bone> it = pending.iterator();
            while (it.hasNext()) {
                Bone bone = it.next();
                if (isResolvable(bone, built)) {
                    built.put(bone.name, buildBone(bone, index, built, data, uvScale, deferred));
                    it.remove();
                }
            }
        }

        if (!pending.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Bone b : pending) names.add(b.name);
            throw new MossGeometryException(
                    "Cyclic or missing parent references among bones: " + names);
        }
    }

    private static boolean isResolvable(Bone bone, Map<String, ModelPartData> built) {
        return bone.parent == null || bone.parent.isEmpty() || built.containsKey(bone.parent);
    }

    private static String pathOf(Bone bone, Map<String, Bone> index) {
        List<String> parts = new ArrayList<>();
        Bone cur = bone;
        while (cur != null) {
            parts.add(cur.name);
            if (cur.parent == null || cur.parent.isEmpty()) break;
            cur = index.get(cur.parent);
        }
        Collections.reverse(parts);
        return String.join("/", parts);
    }

    private static ModelPartData buildBone(Bone bone, Map<String, Bone> index,
                                           Map<String, ModelPartData> built, ModelData data,
                                           float uvScale, List<MossPerFaceCube> deferred) {
        ModelPartData parentPart = resolveParent(bone, built, data);
        ModelTransform boneTransform = buildBoneTransform(bone, index);

        ModelPartBuilder flatBuilder = ModelPartBuilder.create();
        List<ModelPartBuilder> rotatedSubs = new ArrayList<>();
        List<ModelTransform> rotatedTransforms = new ArrayList<>();

        if (bone.cubes != null) {
            Vec3 bonePivot = orZero(bone.pivot);
            for (Cube cube : bone.cubes) {
                if (cube.uv == null) continue;

                boolean hasRotation = cube.hasRotation();
                Vec3 pivot = cube.pivot != null ? cube.pivot : bonePivot;
                ModelPartBuilder target = hasRotation ? ModelPartBuilder.create() : flatBuilder;

                Vec3 origin = orZero(cube.origin);
                Vec3 size = orZero(cube.size);

                // AmbleKit 的坐标公式
                float ox = origin.x - pivot.x;
                float oy = -(origin.y - pivot.y + size.y);
                float oz = origin.z - pivot.z;

                if (cube.uv.isBox()) {
                    target.uv(Math.round(cube.uv.box[0] * uvScale),
                            Math.round(cube.uv.box[1] * uvScale));
                    if (cube.isMirror()) target.mirrored();
                    target.cuboid(ox, oy, oz, size.x, size.y, size.z, new Dilation(cube.inflate));
                    if (cube.isMirror()) target.mirrored(false);
                } else {
                    // per-face：记进 deferred
                    float[] cubePivot = new float[]{ pivot.x, pivot.y, pivot.z };
                    float[] cubeRot = new float[]{
                            cube.rotation != null ? cube.rotation.x : 0f,
                            cube.rotation != null ? cube.rotation.y : 0f,
                            cube.rotation != null ? cube.rotation.z : 0f
                    };
                    deferred.add(new MossPerFaceCube(
                            pathOf(bone, index),
                            ox, oy, oz,
                            size.x, size.y, size.z,
                            cube.inflate, cube.isMirror(),
                            cube.uv,
                            cubePivot, cubeRot
                    ));
                }

                if (hasRotation) {
                    ModelTransform tf = ModelTransform.of(
                            -(bonePivot.x - pivot.x),
                            bonePivot.y - pivot.y,
                            -(bonePivot.z - pivot.z),
                            rad(cube.rotation.x), rad(cube.rotation.y), rad(cube.rotation.z)
                    );
                    rotatedSubs.add(target);
                    rotatedTransforms.add(tf);
                }
            }
        }

        ModelPartData bonePart = parentPart.addChild(bone.name, flatBuilder, boneTransform);

        for (int i = 0; i < rotatedSubs.size(); i++) {
            bonePart.addChild(bone.name + "_r" + i, rotatedSubs.get(i), rotatedTransforms.get(i));
        }

        return bonePart;
    }

    private static ModelPartData resolveParent(Bone bone, Map<String, ModelPartData> built, ModelData data) {
        if (bone.parent == null || bone.parent.isEmpty()) return data.getRoot();
        ModelPartData parent = built.get(bone.parent);
        if (parent == null) {
            throw new MossGeometryException(
                    "Bone '" + bone.name + "' references missing parent '" + bone.parent + "'");
        }
        return parent;
    }

    private static ModelTransform buildBoneTransform(Bone bone, Map<String, Bone> index) {
        Vec3 pivot = orZero(bone.pivot);
        Vec3 rot = orZero(bone.rotation);

        if (bone.parent == null || bone.parent.isEmpty()) {
            // 根骨骼：用绝对 pivot。
            // Y 取反 —— 和子骨骼保持一致的约定，因为渲染时会整体 X 轴 180° 翻转。
            return ModelTransform.of(
                    pivot.x,
                    -pivot.y,
                    pivot.z,
                    rad(rot.x), rad(rot.y), rad(rot.z));
        }

        Vec3 parentPivot = orZero(index.get(bone.parent).pivot);
        float px = -(parentPivot.x - pivot.x);
        float py = parentPivot.y - pivot.y;
        float pz = -(parentPivot.z - pivot.z);

        return ModelTransform.of(px, py, pz, rad(rot.x), rad(rot.y), rad(rot.z));
    }

    private static Vec3 orZero(Vec3 v) { return v == null ? Vec3.ZERO : v; }
    private static float rad(float deg) { return (float) Math.toRadians(deg); }
}