package com.example.doctor_m.mixin;

import dev.amble.ait.core.handles.HandlesResponse;
import dev.amble.ait.registry.impl.HandlesResponseRegistry;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;

@Mixin(HandlesResponseRegistry.class)
public class MixinHandlesItem {

    private static final Map<String, String> CHINESE_TO_ENGLISH = new java.util.HashMap<>();

    static {
        CHINESE_TO_ENGLISH.put("帮助", "help");
        CHINESE_TO_ENGLISH.put("笑话", "tell me a joke");
        CHINESE_TO_ENGLISH.put("冷知识", "tell me a fun fact");
        CHINESE_TO_ENGLISH.put("起飞", "take off");
        CHINESE_TO_ENGLISH.put("出发", "take off");
        CHINESE_TO_ENGLISH.put("启动飞行", "take off");
        CHINESE_TO_ENGLISH.put("飞", "fly");
        CHINESE_TO_ENGLISH.put("走你", "allons-y");
        CHINESE_TO_ENGLISH.put("冲啊", "geronimo");
        CHINESE_TO_ENGLISH.put("解物质化", "demat");
        CHINESE_TO_ENGLISH.put("去物质化", "dematerialize");
        CHINESE_TO_ENGLISH.put("降落", "land");
        CHINESE_TO_ENGLISH.put("着陆", "land");
        CHINESE_TO_ENGLISH.put("停止飞行", "stop flight");
        CHINESE_TO_ENGLISH.put("停飞", "stop flying");
        CHINESE_TO_ENGLISH.put("传送", "displace");
        CHINESE_TO_ENGLISH.put("航点", "waypoint");
        CHINESE_TO_ENGLISH.put("前往航点", "go to waypoint");
        CHINESE_TO_ENGLISH.put("飞往航点", "fly to waypoint");
        CHINESE_TO_ENGLISH.put("切换护盾", "toggle shields");
        CHINESE_TO_ENGLISH.put("护盾", "shields");
        CHINESE_TO_ENGLISH.put("切换警报", "toggle alarms");
        CHINESE_TO_ENGLISH.put("警报", "alarms");
        CHINESE_TO_ENGLISH.put("钟声", "cloister");
        CHINESE_TO_ENGLISH.put("教堂钟", "cloister bells");
        CHINESE_TO_ENGLISH.put("切换反重力", "toggle antigravs");
        CHINESE_TO_ENGLISH.put("反重力", "antigravs");
        CHINESE_TO_ENGLISH.put("重力", "gravity");
        CHINESE_TO_ENGLISH.put("切换隐身", "toggle cloak");
        CHINESE_TO_ENGLISH.put("隐身", "cloak");
        CHINESE_TO_ENGLISH.put("协议3", "p3");
        CHINESE_TO_ENGLISH.put("开门", "open doors");
        CHINESE_TO_ENGLISH.put("打开门", "open the doors");
        CHINESE_TO_ENGLISH.put("打开", "open");
        CHINESE_TO_ENGLISH.put("开门吧", "open sesame");
        CHINESE_TO_ENGLISH.put("关门", "close doors");
        CHINESE_TO_ENGLISH.put("关闭门", "close the doors");
        CHINESE_TO_ENGLISH.put("关闭", "close");
        CHINESE_TO_ENGLISH.put("切换锁", "toggle lock");
        CHINESE_TO_ENGLISH.put("锁门", "lock");
        CHINESE_TO_ENGLISH.put("解锁", "unlock");
        CHINESE_TO_ENGLISH.put("门锁", "door lock");
        CHINESE_TO_ENGLISH.put("拉手刹", "activate handbrake");
        CHINESE_TO_ENGLISH.put("手刹开", "handbrake on");
        CHINESE_TO_ENGLISH.put("开启手刹", "handbrake on");
        CHINESE_TO_ENGLISH.put("放手刹", "disable handbrake");
        CHINESE_TO_ENGLISH.put("手刹关", "handbrake off");
        CHINESE_TO_ENGLISH.put("关闭手刹", "handbrake off");
        CHINESE_TO_ENGLISH.put("开始加油", "enable refuelling");
        CHINESE_TO_ENGLISH.put("加油开", "activate refuel");
        CHINESE_TO_ENGLISH.put("加油", "refuel");
        CHINESE_TO_ENGLISH.put("启动加油", "start refueling");
        CHINESE_TO_ENGLISH.put("加油开", "refueling on");
        CHINESE_TO_ENGLISH.put("停止加油", "stop refueling");
        CHINESE_TO_ENGLISH.put("加油关", "disable refueling");
        CHINESE_TO_ENGLISH.put("禁用加油", "disable refueling");
        CHINESE_TO_ENGLISH.put("关闭加油", "disable refueling");
        CHINESE_TO_ENGLISH.put("进度", "progress");
        CHINESE_TO_ENGLISH.put("飞行状态", "flight status");
        CHINESE_TO_ENGLISH.put("飞行进度", "flight progress");
    }

    // ========== 1. 替换 getSignedContent，处理中文前缀 ==========
    @Redirect(
            method = "onChatMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/message/SignedMessage;getSignedContent()Ljava/lang/String;"
            )
    )
    private static String redirectGetSignedContent(SignedMessage signedMessage) {
        String original = signedMessage.getSignedContent();
        if (original == null || original.isEmpty()) {
            return original;
        }

        String lower = original.toLowerCase();
        String[] chinesePrefixes = {"二把手 ", "驾驶员 ", "手柄 ", "小手柄 "};

        for (String prefix : chinesePrefixes) {
            if (lower.startsWith(prefix.toLowerCase())) {
                // 替换为 "handles " + 剩余部分
                return "handles " + original.substring(prefix.length());
            }
        }

        return original;
    }

    // ========== 2. 注入中文指令到 COMMANDS_CACHE ==========
    @Inject(
            method = "fillCommands",
            at = @At("TAIL")
    )
    private static void onFillCommands(CallbackInfo ci) {
        try {
            Field field = HandlesResponseRegistry.class.getDeclaredField("COMMANDS_CACHE");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, HandlesResponse> cache = (Map<String, HandlesResponse>) field.get(null);

            if (cache == null) return;

            for (Map.Entry<String, String> entry : CHINESE_TO_ENGLISH.entrySet()) {
                HandlesResponse response = cache.get(entry.getValue());
                if (response != null) {
                    cache.put(entry.getKey(), response);
                }
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}