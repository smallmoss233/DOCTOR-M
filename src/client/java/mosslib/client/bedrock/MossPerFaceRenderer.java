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
import java.util.List;

public final class MossPerFaceRenderer {

    private MossPerFaceRenderer() {}

    public static void render(ModelPart root,
                              List<MossPerFaceCube> deferred,
                              MatrixStack matrices,
                              VertexConsumer vertices,
                              int light, int overlay,
                              float r, float g, float b, float a,
                              int texW, int texH) {

        // 进入 1/16 单位的模型空间
        matrices.push();
        matrices.scale(1f / 16f, 1f / 16f, 1f / 16f);

        for (MossPerFaceCube cube : deferred) {
            ModelPart bone = resolveBone(root, cube.bonePath());
            if (bone == null) continue;

            matrices.push();

            // 骨骼当前变换（默认 = 转换时写入的 pivot + 旋转；动画会改这些值）
            applyBoneTransform(matrices, bone);

            // cube 相对骨骼或 cube pivot 的偏移
            float[] pivot = cube.cubePivot();
            matrices.translate(pivot[0], pivot[1], pivot[2]);

            float[] rot = cube.cubeRotation();
            if (rot[0] != 0f) matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.toRadians(rot[0])));
            if (rot[1] != 0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.toRadians(rot[1])));
            if (rot[2] != 0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.toRadians(rot[2])));

            MatrixStack.Entry entry = matrices.peek();
            for (MossPerFaceQuad q : buildQuads(cube, texW, texH)) {
                q.render(entry, vertices, light, overlay, r, g, b, a);
            }

            matrices.pop();
        }

        matrices.pop();
    }

    /** 把 ModelPart 自身的 pivot + 旋转加到矩阵上。以后动画改 part 的字段会自动生效。 */
    private static void applyBoneTransform(MatrixStack matrices, ModelPart part) {
        matrices.translate(part.pivotX, part.pivotY, part.pivotZ);
        if (part.roll  != 0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotation(part.roll));
        if (part.yaw   != 0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(part.yaw));
        if (part.pitch != 0f) matrices.multiply(RotationAxis.POSITIVE_X.rotation(part.pitch));
    }

    /** 沿 "a/b/c" 路径逐级 getChild */
    private static ModelPart resolveBone(ModelPart root, String path) {
        if (path == null || path.isEmpty()) return root;
        ModelPart cur = root;
        for (String seg : path.split("/")) {
            if (!cur.hasChild(seg)) return null;
            cur = cur.getChild(seg);
        }
        return cur;
    }

    // ─── 6 个面拆成独立 quad ────────────────────────

    public static List<MossPerFaceQuad> buildQuads(MossPerFaceCube cube, int texW, int texH) {
        List<MossPerFaceQuad> out = new ArrayList<>(6);

        float x0 = cube.x() - cube.inflate();
        float y0 = cube.y() - cube.inflate();
        float z0 = cube.z() - cube.inflate();
        float x1 = cube.x() + cube.sizeX() + cube.inflate();
        float y1 = cube.y() + cube.sizeY() + cube.inflate();
        float z1 = cube.z() + cube.sizeZ() + cube.inflate();

        emit(out, cube.uv().north, Dir.NORTH, cube.mirror(), x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, cube.uv().south, Dir.SOUTH, cube.mirror(), x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, cube.uv().east,  Dir.EAST,  cube.mirror(), x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, cube.uv().west,  Dir.WEST,  cube.mirror(), x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, cube.uv().up,    Dir.UP,    cube.mirror(), x0, y0, z0, x1, y1, z1, texW, texH);
        emit(out, cube.uv().down,  Dir.DOWN,  cube.mirror(), x0, y0, z0, x1, y1, z1, texW, texH);

        return out;
    }

    private enum Dir { NORTH, SOUTH, EAST, WEST, UP, DOWN }

    private static void emit(List<MossPerFaceQuad> out, MossGeometry.UV.Face face, Dir dir,
                             boolean mirror,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             int texW, int texH) {
        if (face == null || face.uv() == null || face.uv().length < 2) return;

        float u = face.uv()[0], v = face.uv()[1];
        float w = (face.uvSize() != null && face.uvSize().length >= 2) ? face.uvSize()[0] : 1f;
        float h = (face.uvSize() != null && face.uvSize().length >= 2) ? face.uvSize()[1] : 1f;

        float u0 = u / texW, v0 = v / texH;
        float u1 = (u + w) / texW, v1 = (v + h) / texH;
        if (mirror) { float t = u0; u0 = u1; u1 = t; }

        Vector3f p0, p1, p2, p3;
        Vector3f normal;
        float uu0, vv0, uu1, vv1;

        switch (dir) {
            case NORTH -> {
                p0 = new Vector3f(x0, y0, z0);
                p1 = new Vector3f(x1, y0, z0);
                p2 = new Vector3f(x1, y1, z0);
                p3 = new Vector3f(x0, y1, z0);
                normal = new Vector3f(0, 0, -1);
                uu0 = u0; vv0 = v1; uu1 = u1; vv1 = v0;
            }
            case SOUTH -> {
                p0 = new Vector3f(x1, y0, z1);
                p1 = new Vector3f(x0, y0, z1);
                p2 = new Vector3f(x0, y1, z1);
                p3 = new Vector3f(x1, y1, z1);
                normal = new Vector3f(0, 0, 1);
                uu0 = u0; vv0 = v1; uu1 = u1; vv1 = v0;
            }
            case EAST -> {
                p0 = new Vector3f(x1, y0, z1);
                p1 = new Vector3f(x1, y0, z0);
                p2 = new Vector3f(x1, y1, z0);
                p3 = new Vector3f(x1, y1, z1);
                normal = new Vector3f(1, 0, 0);
                uu0 = u0; vv0 = v1; uu1 = u1; vv1 = v0;
            }
            case WEST -> {
                p0 = new Vector3f(x0, y0, z0);
                p1 = new Vector3f(x0, y0, z1);
                p2 = new Vector3f(x0, y1, z1);
                p3 = new Vector3f(x0, y1, z0);
                normal = new Vector3f(-1, 0, 0);
                uu0 = u0; vv0 = v1; uu1 = u1; vv1 = v0;
            }
            case UP -> {
                p0 = new Vector3f(x0, y1, z0);
                p1 = new Vector3f(x1, y1, z0);
                p2 = new Vector3f(x1, y1, z1);
                p3 = new Vector3f(x0, y1, z1);
                normal = new Vector3f(0, 1, 0);
                uu0 = u0; vv0 = v1; uu1 = u1; vv1 = v0;
            }
            case DOWN -> {
                p0 = new Vector3f(x0, y0, z1);
                p1 = new Vector3f(x1, y0, z1);
                p2 = new Vector3f(x1, y0, z0);
                p3 = new Vector3f(x0, y0, z0);
                normal = new Vector3f(0, -1, 0);
                uu0 = u0; vv0 = v1; uu1 = u1; vv1 = v0;
            }
            default -> { return; }
        }

        out.add(new MossPerFaceQuad(p0, p1, p2, p3, uu0, vv0, uu1, vv1, normal));
    }
}