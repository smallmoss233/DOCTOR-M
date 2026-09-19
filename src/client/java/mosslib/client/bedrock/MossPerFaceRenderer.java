/*
 * Portions of this file are derived from AmbleKit
 * (dev.amble.lib.client.bedrock), Copyright (C) 2025 AmbleLabs,
 * licensed under the Mozilla Public License, v. 2.0.
 * You may obtain a copy of the MPL at https://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package mosslib.client.bedrock;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class MossPerFaceRenderer {

    private static final float BEDROCK_UNIT = 1.0F / 16.0F;

    private static final int FACES_PER_CUBE = 6;

    private static final Map<ModelPart, Map<String, ModelPart>> BONE_PATH_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MossPerFaceRenderer() {}

    public static void render(ModelPart root,
                              List<MossPerFaceCube> deferred,
                              MatrixStack matrices,
                              VertexConsumer vertices,
                              int light, int overlay,
                              float red, float green, float blue, float alpha,
                              int textureWidth, int textureHeight) {

        if (deferred.isEmpty()) {
            return;
        }

        matrices.push();
        try {
            matrices.scale(BEDROCK_UNIT, BEDROCK_UNIT, BEDROCK_UNIT);

            List<MossPerFaceQuad> quads = new ArrayList<>(FACES_PER_CUBE);

            for (MossPerFaceCube cube : deferred) {
                ModelPart bone = getBoneByPath(root, cube.bonePath());
                if (bone == null) {
                    continue;
                }

                matrices.push();
                try {
                    applyCubeTransform(matrices, bone, cube);

                    MatrixStack.Entry entry = matrices.peek();

                    quads.clear();
                    buildQuadsInto(quads, cube, textureWidth, textureHeight);
                    for (int i = 0, n = quads.size(); i < n; i++) {
                        quads.get(i).render(entry, vertices, light, overlay,
                                red, green, blue, alpha);
                    }
                } finally {
                    matrices.pop();
                }
            }
        } finally {
            matrices.pop();
        }
    }

    // 变换
    private static void applyCubeTransform(MatrixStack matrices, ModelPart bone, MossPerFaceCube cube) {
        float[] pivot = cube.cubePivot();
        float[] rot = cube.cubeRotation();

        matrices.scale(bone.xScale, bone.yScale, bone.zScale);
        matrices.translate(pivot[0], -pivot[1], pivot[2]);

        matrices.multiply(RotationAxis.POSITIVE_X.rotation(
                bone.pitch == 0.0F ? (float) Math.toRadians(rot[0]) : bone.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(
                bone.yaw == 0.0F ? (float) Math.toRadians(rot[1]) : bone.yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(
                bone.roll == 0.0F ? (float) Math.toRadians(rot[2]) : bone.roll));
    }

    // 骨骼路径解析
    private static ModelPart getBoneByPath(ModelPart root, String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }

        Map<String, ModelPart> cache = BONE_PATH_CACHE.computeIfAbsent(
                root, r -> new ConcurrentHashMap<>());

        ModelPart cached = cache.get(path);
        if (cached != null) {
            return cached;
        }

        ModelPart resolved = resolveBonePath(root, path);
        if (resolved != null) {
            cache.put(path, resolved);
        }
        return resolved;
    }

    /** 不使用 {@code split} 的路径解析实现，避免正则编译和临时数组。 */
    private static ModelPart resolveBonePath(ModelPart root, String path) {
        ModelPart current = root;
        int start = 0;
        final int length = path.length();

        while (start < length) {
            int end = path.indexOf('/', start);
            if (end < 0) {
                end = length;
            }

            String segment = path.substring(start, end);
            if (!current.hasChild(segment)) {
                return null;
            }
            current = current.getChild(segment);
            start = end + 1;
        }
        return current;
    }

    // 四边形构建
    /** 保留旧 API：为单个立方体构建其所有面。 */
    public static List<MossPerFaceQuad> buildQuads(MossPerFaceCube cube, int texW, int texH) {
        List<MossPerFaceQuad> quads = new ArrayList<>(FACES_PER_CUBE);
        buildQuadsInto(quads, cube, texW, texH);
        return quads;
    }

    /** 内部版本：写入调用方提供的列表，避免热路径中反复分配。 */
    private static void buildQuadsInto(List<MossPerFaceQuad> out, MossPerFaceCube cube,
                                       int texW, int texH) {
        float inflate = cube.inflate();
        float x0 = cube.x() - inflate;
        float y0 = cube.y() - inflate;
        float z0 = cube.z() - inflate;
        float x1 = cube.x() + cube.sizeX() + inflate;
        float y1 = cube.y() + cube.sizeY() + inflate;
        float z1 = cube.z() + cube.sizeZ() + inflate;

        boolean mirror = cube.mirror();
        MossGeometry.UV uv = cube.uv();

        emit(out, uv.north, FaceDir.NORTH, mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, uv.south, FaceDir.SOUTH, mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        // 注意：以下两个映射（east↔west）是 Bedrock 约定下刻意保留的，不要修改。
        emit(out, uv.east,  FaceDir.WEST,  mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, uv.west,  FaceDir.EAST,  mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, uv.up,    FaceDir.UP,    mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, uv.down,  FaceDir.DOWN,  mirror, x0, y0, z0, x1, y1, z1, texW, texH);
    }

    /** 面方向枚举，替代字符串 switch，避免每次哈希。 */
    private enum FaceDir { NORTH, SOUTH, EAST, WEST, UP, DOWN }

    private static void emit(List<MossPerFaceQuad> out, MossGeometry.UV.Face face, FaceDir dir,
                             boolean mirror,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             int texW, int texH) {
        if (face == null || face.uv() == null || face.uv().length < 2) {
            return;
        }

        float u = face.uv()[0];
        float v = face.uv()[1];

        float w = (face.uvSize() != null && face.uvSize().length >= 2) ? face.uvSize()[0] : 1f;
        float h = (face.uvSize() != null && face.uvSize().length >= 2) ? face.uvSize()[1] : 1f;

        float u0 = u / texW;
        float v0 = v / texH;
        float u1 = (u + w) / texW;
        float v1 = (v + h) / texH;

        if (mirror) {
            float t = u0; u0 = u1; u1 = t;
        }

        switch (dir) {
            case NORTH -> out.add(new MossPerFaceQuad(
                    new Vector3f(x1, y0, z0), new Vector3f(x0, y0, z0),
                    new Vector3f(x0, y1, z0), new Vector3f(x1, y1, z0),
                    u1, v0, u0, v1,
                    new Vector3f(0, 0, -1)
            ));
            case SOUTH -> out.add(new MossPerFaceQuad(
                    new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1),
                    new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1),
                    u1, v0, u0, v1,
                    new Vector3f(0, 0, 1)
            ));
            case EAST -> out.add(new MossPerFaceQuad(
                    new Vector3f(x1, y0, z1), new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y1, z0), new Vector3f(x1, y1, z1),
                    u1, v0, u0, v1,
                    new Vector3f(1, 0, 0)
            ));
            case WEST -> out.add(new MossPerFaceQuad(
                    new Vector3f(x0, y0, z0), new Vector3f(x0, y0, z1),
                    new Vector3f(x0, y1, z1), new Vector3f(x0, y1, z0),
                    u1, v0, u0, v1,
                    new Vector3f(-1, 0, 0)
            ));
            case UP -> out.add(new MossPerFaceQuad(
                    new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y0, z1), new Vector3f(x0, y0, z1),
                    u0, v1, u1, v0,
                    new Vector3f(0, -1, 0)
            ));
            case DOWN -> out.add(new MossPerFaceQuad(
                    new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1),
                    new Vector3f(x1, y1, z0), new Vector3f(x0, y1, z0),
                    u0, v1, u1, v0,
                    new Vector3f(0, 1, 0)
            ));
        }
    }
}