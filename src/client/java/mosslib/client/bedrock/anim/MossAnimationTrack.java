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

import org.joml.Vector3f;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** 一个通道的关键帧序列。时间是秒，值是三元向量。 */
public final class MossAnimationTrack {

    /** time(秒) -> 值 */
    private final NavigableMap<Double, Vector3f> frames = new TreeMap<>();

    public void put(double time, Vector3f value) {
        frames.put(time, value);
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    /**
     * 在给定时间求值。线性插值；边界外取最近帧。
     * Bedrock 的 catmullrom 模式暂不支持，先线性——视觉上大部分动画差别很小。
     */
    public Vector3f resolve(double time) {
        if (frames.isEmpty()) return new Vector3f();

        Map.Entry<Double, Vector3f> before = frames.floorEntry(time);
        Map.Entry<Double, Vector3f> after = frames.ceilingEntry(time);

        // 时间在首帧之前
        if (before == null) return new Vector3f(after.getValue());
        // 时间在末帧之后
        if (after == null) return new Vector3f(before.getValue());
        // 恰好落在某一帧
        if (before.getKey().equals(after.getKey())) return new Vector3f(before.getValue());

        double span = after.getKey() - before.getKey();
        float alpha = (float) ((time - before.getKey()) / span);

        Vector3f a = before.getValue();
        Vector3f b = after.getValue();
        return new Vector3f(
                a.x + (b.x - a.x) * alpha,
                a.y + (b.y - a.y) * alpha,
                a.z + (b.z - a.z) * alpha);
    }
}