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

import net.minecraft.client.model.ModelPart;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 无状态播放器：给定动画和时间，把通道值写进 ModelPart 树。
 * 调用方负责每帧先 resetTransform。
 *
 * <p>骨骼名字 → ModelPart 的映射按 root 缓存。由于 ModelPart 没有名字，
 * 只能用 {@code traverse() + hasChild} 暴力查找，结果缓存到 WeakHashMap。</p>
 */
public final class MossAnimationPlayer {

    private MossAnimationPlayer() {}

    /** root ModelPart → (bone name → ModelPart)。WeakHashMap 让树被回收时自动清理。 */
    private static final Map<ModelPart, Map<String, ModelPart>> INDEX_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static void apply(ModelPart root, MossAnimation anim, double elapsedSeconds) {
        double t = anim.fold(elapsedSeconds);

        Map<String, ModelPart> index = INDEX_CACHE.computeIfAbsent(
                root, r -> Collections.synchronizedMap(new HashMap<>()));

        for (Map.Entry<String, MossAnimation.BoneTimeline> e : anim.bones.entrySet()) {
            String name = e.getKey();

            ModelPart bone = index.get(name);
            if (bone == null) {
                bone = findBone(root, name);
                if (bone == null) continue;
                index.put(name, bone);
            }

            MossAnimation.BoneTimeline timeline = e.getValue();

            // rotation: Bedrock 用度，ModelPart 用弧度；Y 轴方向相反
            if (!timeline.rotation.isEmpty()) {
                Vector3f r = timeline.rotation.resolve(t);
                bone.pitch += (float) Math.toRadians(r.x);
                bone.yaw   += (float) Math.toRadians(-r.y);
                bone.roll  += (float) Math.toRadians(r.z);
            }

            // position: Bedrock 的 Y 向下，取反
            if (!timeline.position.isEmpty()) {
                Vector3f p = timeline.position.resolve(t);
                bone.pivotX += p.x;
                bone.pivotY += -p.y;
                bone.pivotZ += p.z;
            }

            // scale: 直接乘
            if (!timeline.scale.isEmpty()) {
                Vector3f s = timeline.scale.resolve(t);
                bone.xScale *= s.x;
                bone.yScale *= s.y;
                bone.zScale *= s.z;
            }
        }
    }

    /** 在 root 的所有后代中查找名为 name 的子节点。 */
    private static ModelPart findBone(ModelPart root, String name) {
        return root.traverse()
                .filter(p -> p.hasChild(name))
                .findFirst()
                .map(p -> p.getChild(name))
                .orElse(null);
    }
}