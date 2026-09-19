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

import mosslib.client.bedrock.MossGeometry.UV;

public record MossPerFaceCube(
        String bonePath,          // 从 root 开始的路径，如 "bb_main/zhongbu"
        float x, float y, float z,
        float sizeX, float sizeY, float sizeZ,
        float inflate,
        boolean mirror,
        UV uv,
        float[] cubePivot,        // 绝对坐标
        float[] cubeRotation      // 度
) {}