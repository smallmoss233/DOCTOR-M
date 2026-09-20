/*
 * Copyright (C) 2025 AmbleLabs
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * This code is MPL, due to it referencing this code:
 * https://gitlab.com/cable-mc/cobblemon/-/blob/main/common/src/main/kotlin/com/cobblemon/mod/common/client/render/models/blockbench/bedrock/animation/BedrockAnimation.kt
 */
package mosslib.client.bedrock.anim;

import java.util.Map;

public final class MossAnimation {
    public enum Loop { LOOP, HOLD, NONE }

    public final String name;
    public final double lengthSeconds;
    public final Loop loop;
    public final Map<String, BoneTimeline> bones;

    public MossAnimation(String name, double lengthSeconds, Loop loop,
                         Map<String, BoneTimeline> bones) {
        this.name = name;
        this.lengthSeconds = lengthSeconds;
        this.loop = loop;
        this.bones = bones;
    }

    public double fold(double elapsed) {
        return switch (loop) {
            case LOOP -> lengthSeconds <= 0 ? 0 : elapsed % lengthSeconds;
            case HOLD -> Math.min(elapsed, lengthSeconds);
            case NONE -> elapsed;
        };
    }

    public static final class BoneTimeline {
        public final MossAnimationTrack rotation;   // ★ 改这里
        public final MossAnimationTrack position;   // ★ 改这里
        public final MossAnimationTrack scale;      // ★ 改这里

        public BoneTimeline(MossAnimationTrack rotation,
                            MossAnimationTrack position,
                            MossAnimationTrack scale) {
            this.rotation = rotation;
            this.position = position;
            this.scale = scale;
        }
    }
}