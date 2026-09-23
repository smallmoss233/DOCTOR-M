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
package mosslib.client.bedrock.block;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class MossPerFaceRenderer {

    private static final float BEDROCK_UNIT = 1.0F / 16.0F;

    private static final int FACES_PER_CUBE = 6;

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
                matrices.push();
                try {
                    // 1) 沿骨骼链逐级应用祖先变换
                    if (!applyBoneChain(root, cube.bonePath(), matrices)) {
                        continue;
                    }

                    // ★ 2) 应用 cube 相对骨骼的 pivot 偏移
                    float[] cpp = cube.cubePivot();
                    if (cpp[0] != 0F || cpp[1] != 0F || cpp[2] != 0F) {
                        matrices.translate(cpp[0], -cpp[1], cpp[2]);
                    }

                    // 3) cube 自身的静态旋转
                    float[] rot = cube.cubeRotation();
                    if (rot[0] != 0F) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotation(
                                (float) Math.toRadians(rot[0])));
                    }
                    if (rot[1] != 0F) {
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(
                                (float) Math.toRadians(rot[1])));
                    }
                    if (rot[2] != 0F) {
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(
                                (float) Math.toRadians(rot[2])));
                    }

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

    // =====================================================================
    // 骨骼链 —— 沿 bonePath 逐级应用 pivot / rotation / scale
    // =====================================================================

    /**
     * 从 root 沿 "A/B/C" 路径走到目标骨骼，每级应用：
     * <ol>
     *   <li>{@code translate(pivot)} —— ModelPart 里已经是相对父级的偏移</li>
     *   <li>{@code rotate(pitch, yaw, roll)} —— 弧度</li>
     *   <li>{@code scale(xScale, yScale, zScale)} —— 非 1 时才应用</li>
     * </ol>
     *
     * @return false 表示路径中途断裂（骨骼名对不上）
     */
    private static boolean applyBoneChain(ModelPart root, String path, MatrixStack matrices) {
        if (path == null || path.isEmpty()) {
            return true;
        }

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
                return false;
            }
            current = current.getChild(segment);

            // pivot
            matrices.translate(current.pivotX, current.pivotY, current.pivotZ);

            // ★ rotation —— 顺序改成 Z → Y → X，和 Minecraft ModelPart 一致
            if (current.roll != 0F) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotation(current.roll));
            }
            if (current.yaw != 0F) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(current.yaw));
            }
            if (current.pitch != 0F) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotation(current.pitch));
            }

            // scale
            if (current.xScale != 1F || current.yScale != 1F || current.zScale != 1F) {
                matrices.scale(current.xScale, current.yScale, current.zScale);
            }

            start = end + 1;
        }

        return true;
    }

    // =====================================================================
    // 四边形构建
    // =====================================================================

    /** 保留旧 API：为单个立方体构建其所有面。 */
    public static List<MossPerFaceQuad> buildQuads(MossPerFaceCube cube, int texW, int texH) {
        List<MossPerFaceQuad> quads = new ArrayList<>(FACES_PER_CUBE);
        buildQuadsInto(quads, cube, texW, texH);
        return quads;
    }

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
        emit(out, uv.east,  FaceDir.WEST,  mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, uv.west,  FaceDir.EAST,  mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, uv.up,    FaceDir.UP,    mirror, x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, uv.down,  FaceDir.DOWN,  mirror, x0, y0, z0, x1, y1, z1, texW, texH);
    }

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