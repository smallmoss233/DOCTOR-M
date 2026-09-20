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

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record MossPerFaceQuad(
        Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
        float u0, float v0, float u1, float v1,
        Vector3f normal
) {
    public void render(MatrixStack.Entry entry, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        Matrix4f pos = entry.getPositionMatrix();
        Matrix3f nrm = entry.getNormalMatrix();

        Vector3f n = new Vector3f(normal).mul(nrm);

        vertex(vertices, pos, p0, u0, v0, n, light, overlay, red, green, blue, alpha);
        vertex(vertices, pos, p1, u1, v0, n, light, overlay, red, green, blue, alpha);
        vertex(vertices, pos, p2, u1, v1, n, light, overlay, red, green, blue, alpha);
        vertex(vertices, pos, p3, u0, v1, n, light, overlay, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pos, Vector3f p,
                               float u, float v, Vector3f n,
                               int light, int overlay,
                               float red, float green, float blue, float alpha) {
        vc.vertex(pos, p.x(), p.y(), p.z())
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(n.x(), n.y(), n.z())
                .next();
    }
}