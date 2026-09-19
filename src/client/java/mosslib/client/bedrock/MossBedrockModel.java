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
import net.minecraft.client.model.TexturedModelData;

import java.util.List;

public record MossBedrockModel(
        TexturedModelData data,
        List<MossPerFaceCube> deferred,
        int textureWidth,
        int textureHeight
) {
    public ModelPart createModel() {
        return data.createModel();
    }
}