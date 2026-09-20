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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class MossAnimationLoader {

    private MossAnimationLoader() {}

    /** 从一整个 .animation.json 加载，返回 animationName -> MossAnimation。 */
    public static Map<String, MossAnimation> loadAll(InputStreamReader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonObject anims = root.getAsJsonObject("animations");
        Map<String, MossAnimation> out = new HashMap<>();
        if (anims == null) return out;

        for (Map.Entry<String, JsonElement> entry : anims.entrySet()) {
            String name = entry.getKey();
            out.put(name, parseOne(name, entry.getValue().getAsJsonObject()));
        }
        return out;
    }

    private static MossAnimation parseOne(String name, JsonObject json) {
        double length = json.has("animation_length")
                ? json.get("animation_length").getAsDouble() : 1.0;

        MossAnimation.Loop loop = MossAnimation.Loop.NONE;
        if (json.has("loop")) {
            JsonElement l = json.get("loop");
            if (l.isJsonPrimitive() && l.getAsJsonPrimitive().isBoolean()) {
                loop = l.getAsBoolean() ? MossAnimation.Loop.LOOP : MossAnimation.Loop.NONE;
            } else if (l.isJsonPrimitive() && l.getAsJsonPrimitive().isString()) {
                String s = l.getAsString();
                if ("hold_on_last_frame".equals(s)) loop = MossAnimation.Loop.HOLD;
                else if ("true".equalsIgnoreCase(s)) loop = MossAnimation.Loop.LOOP;
            }
        }

        Map<String, MossAnimation.BoneTimeline> bones = new HashMap<>();
        JsonObject bonesJson = json.getAsJsonObject("bones");
        if (bonesJson != null) {
            for (Map.Entry<String, JsonElement> e : bonesJson.entrySet()) {
                bones.put(e.getKey(), parseBone(e.getValue().getAsJsonObject()));
            }
        }

        return new MossAnimation(name, length, loop, bones);
    }

    private static MossAnimation.BoneTimeline parseBone(JsonObject bone) {
        return new MossAnimation.BoneTimeline(
                parseTrack(bone.get("rotation")),
                parseTrack(bone.get("position")),
                parseTrack(bone.get("scale")));
    }

    private static MossAnimationTrack parseTrack(JsonElement json) {
        MossAnimationTrack track = new MossAnimationTrack();
        if (json == null || json.isJsonNull()) return track;

        // 静态数组 [x, y, z] —— 整段恒定
        if (json.isJsonArray()) {
            track.put(0.0, readVec(json.getAsJsonArray()));
            return track;
        }

        // 关键帧对象 { "0.0": [...], "1.0": [...] }
        JsonObject obj = json.getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            // 忽略 "lerp_mode" 之类的非时间键
            if (!e.getKey().matches("-?\\d+(\\.\\d+)?")) continue;

            double time = Double.parseDouble(e.getKey());
            JsonElement value = e.getValue();

            // 关键帧可能是 { "post": [...], "pre": [...] } 形式
            if (value.isJsonObject()) {
                JsonObject kf = value.getAsJsonObject();
                JsonElement arr = kf.has("post") ? kf.get("post") : kf.get("pre");
                if (arr != null) track.put(time, readVec(arr.getAsJsonArray()));
            } else {
                track.put(time, readVec(value.getAsJsonArray()));
            }
        }
        return track;
    }

    private static org.joml.Vector3f readVec(com.google.gson.JsonArray arr) {
        return new org.joml.Vector3f(
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat());
    }
}