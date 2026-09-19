package mosslib.client.bedrock;

import mosslib.client.bedrock.MossGeometry.*;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;

import java.util.*;

@Environment(EnvType.CLIENT)
final class MossGeometryConverter {

    /** 薄片厚度：极小值，视觉上不可见 */
    private static final float THIN = 0.001f;

    private MossGeometryConverter() {}

    static TexturedModelData convert(MossGeometry model, String wantedGeometry) {
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

        buildBones(geometry.bones, boneIndex, built, data);

        return TexturedModelData.of(data, desc.textureWidth, desc.textureHeight);
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

    private static void buildBones(List<Bone> bones, Map<String, Bone> index,
                                   Map<String, ModelPartData> built, ModelData data) {
        List<Bone> pending = new ArrayList<>(bones);
        int guard = pending.size() + 1;

        while (!pending.isEmpty() && guard-- > 0) {
            Iterator<Bone> it = pending.iterator();
            while (it.hasNext()) {
                Bone bone = it.next();
                if (isResolvable(bone, built)) {
                    built.put(bone.name, buildBone(bone, index, built, data));
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

    private static ModelPartData buildBone(Bone bone, Map<String, Bone> index,
                                           Map<String, ModelPartData> built, ModelData data) {
        ModelPartData parentPart = resolveParent(bone, built, data);
        ModelTransform boneTransform = buildBoneTransform(bone, index);

        ModelPartBuilder flatBuilder = ModelPartBuilder.create();
        List<Cube> perFaceCubes = new ArrayList<>();
        List<Cube> rotatedCubes = new ArrayList<>();

        if (bone.cubes != null) {
            Vec3 bonePivot = orZero(bone.pivot);
            for (Cube cube : bone.cubes) {
                if (cube.uv == null) continue;

                boolean isRotated = cube.rotation != null && !cube.rotation.isZero();

                if (isRotated) {
                    rotatedCubes.add(cube);
                } else if (cube.uv.isBox()) {
                    addBoxCube(flatBuilder, cube, bonePivot, bone.name);
                } else {
                    perFaceCubes.add(cube);
                }
            }
        }

        ModelPartData bonePart = parentPart.addChild(bone.name, flatBuilder, boneTransform);

        for (int i = 0; i < perFaceCubes.size(); i++) {
            addPerFaceCube(bonePart, bone, perFaceCubes.get(i), "pf" + i);
        }
        for (int i = 0; i < rotatedCubes.size(); i++) {
            addRotatedCube(bonePart, bone, rotatedCubes.get(i), i);
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
            return ModelTransform.of(0f, 0f, 0f, rad(rot.x), rad(rot.y), rad(rot.z));
        }

        Vec3 parentPivot = orZero(index.get(bone.parent).pivot);
        float px = pivot.x - parentPivot.x;
        float py = parentPivot.y - pivot.y;
        float pz = pivot.z - parentPivot.z;

        return ModelTransform.of(px, py, pz, rad(rot.x), rad(rot.y), rad(rot.z));
    }

    // ─── Box UV cube ──────────────────────────────────

    private static void addBoxCube(ModelPartBuilder builder, Cube cube, Vec3 pivotRef, String boneName) {
        Vec3 origin = orZero(cube.origin);
        Vec3 size = orZero(cube.size);

        float ox = origin.x - pivotRef.x;
        float oy = -(origin.y - pivotRef.y + size.y);
        float oz = origin.z - pivotRef.z;

        builder.uv(cube.uv.box[0], cube.uv.box[1]);
        builder.mirrored(cube.mirror);
        builder.cuboid(ox, oy, oz, size.x, size.y, size.z, new Dilation(cube.inflate));
        builder.mirrored(false);
    }

    // ─── Per-face UV cube：6 个薄片 ──────────────────

    private static void addPerFaceCube(ModelPartData bonePart, Bone bone, Cube cube, String tag) {
        Vec3 bonePivot = orZero(bone.pivot);
        Vec3 origin = orZero(cube.origin);
        Vec3 size = orZero(cube.size);

        float ox = origin.x - bonePivot.x;
        float oy = -(origin.y - bonePivot.y + size.y);
        float oz = origin.z - bonePivot.z;

        float w = size.x;
        float h = size.y;
        float d = size.z;

        UV uv = cube.uv;
        Dilation dil = new Dilation(cube.inflate);
        boolean mir = cube.mirror;
        ModelTransform zero = ModelTransform.pivot(0f, 0f, 0f);

        if (uv.north != null) {
            int[] p = faceUV(uv.north);
            ModelPartBuilder b = ModelPartBuilder.create();
            b.uv(p[0], p[1]);
            b.mirrored(mir);
            b.cuboid(ox, oy, oz - THIN, w, h, THIN, dil);
            b.mirrored(false);
            bonePart.addChild(bone.name + "_" + tag + "_n", b, zero);
        }

        if (uv.south != null) {
            int[] p = faceUV(uv.south);
            ModelPartBuilder b = ModelPartBuilder.create();
            b.uv(p[0] - (int) w, p[1]);
            b.mirrored(mir);
            b.cuboid(ox, oy, oz + d, w, h, THIN, dil);
            b.mirrored(false);
            bonePart.addChild(bone.name + "_" + tag + "_s", b, zero);
        }

        if (uv.east != null) {
            int[] p = faceUV(uv.east);
            ModelPartBuilder b = ModelPartBuilder.create();
            b.uv(p[0], p[1] - (int) d);
            b.mirrored(mir);
            b.cuboid(ox + w, oy, oz, THIN, h, d, dil);
            b.mirrored(false);
            bonePart.addChild(bone.name + "_" + tag + "_e", b, zero);
        }

        if (uv.west != null) {
            int[] p = faceUV(uv.west);
            ModelPartBuilder b = ModelPartBuilder.create();
            b.uv(p[0] - (int) d, p[1] - (int) d);
            b.mirrored(mir);
            b.cuboid(ox - THIN, oy, oz, THIN, h, d, dil);
            b.mirrored(false);
            bonePart.addChild(bone.name + "_" + tag + "_w", b, zero);
        }

        if (uv.up != null) {
            int[] p = faceUV(uv.up);
            ModelPartBuilder b = ModelPartBuilder.create();
            b.uv(p[0] - (int) d, p[1]);
            b.mirrored(mir);
            b.cuboid(ox, oy + h, oz, w, THIN, d, dil);
            b.mirrored(false);
            bonePart.addChild(bone.name + "_" + tag + "_u", b, zero);
        }

        if (uv.down != null) {
            int[] p = faceUV(uv.down);
            ModelPartBuilder b = ModelPartBuilder.create();
            b.uv(p[0] - (int) d - (int) w, p[1]);
            b.mirrored(mir);
            b.cuboid(ox, oy - THIN, oz, w, THIN, d, dil);
            b.mirrored(false);
            bonePart.addChild(bone.name + "_" + tag + "_d", b, zero);
        }
    }

    private static int[] faceUV(UV.Face face) {
        if (face == null || face.uv() == null || face.uv().length < 2) return new int[]{0, 0};
        return new int[]{(int) face.uv()[0], (int) face.uv()[1]};
    }

    // ─── Rotated cube（per-face 降级） ────────────────

    private static void addRotatedCube(ModelPartData bonePart, Bone bone, Cube cube, int index) {
        Vec3 bonePivot = orZero(bone.pivot);
        Vec3 cubePivot = orZero(cube.pivot);

        float px = cubePivot.x - bonePivot.x;
        float py = bonePivot.y - cubePivot.y;
        float pz = cubePivot.z - bonePivot.z;

        Vec3 rot = orZero(cube.rotation);

        int u, v;
        if (cube.uv.isBox()) {
            u = cube.uv.box[0];
            v = cube.uv.box[1];
        } else {
            UV.Face north = cube.uv.north;
            if (north == null || north.uv() == null || north.uv().length < 2) {
                throw new MossGeometryException(
                        "Bone '" + bone.name + "': rotated per-face cube missing 'north'");
            }
            int d = (int) orZero(cube.size).z;
            u = (int) north.uv()[0] - d;
            v = (int) north.uv()[1] - d;
        }

        Vec3 origin = orZero(cube.origin);
        Vec3 size = orZero(cube.size);

        float ox = origin.x - cubePivot.x;
        float oy = -(origin.y - cubePivot.y + size.y);
        float oz = origin.z - cubePivot.z;

        ModelPartBuilder builder = ModelPartBuilder.create();
        builder.uv(u, v);
        builder.mirrored(cube.mirror);
        builder.cuboid(ox, oy, oz, size.x, size.y, size.z, new Dilation(cube.inflate));
        builder.mirrored(false);

        ModelTransform tf = ModelTransform.of(px, py, pz, rad(rot.x), rad(rot.y), rad(rot.z));
        bonePart.addChild(bone.name + "_r" + index, builder, tf);
    }

    // ─── 小工具 ──────────────────────────────────────

    private static Vec3 orZero(Vec3 v) {
        return v == null ? Vec3.ZERO : v;
    }

    private static float rad(float deg) {
        return (float) Math.toRadians(deg);
    }
}