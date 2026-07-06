package com.example.doctor_m.mixin.doctor_m; // 换成你的包名

import dev.amble.ait.module.planet.core.space.planet.Planet;
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry;
import dev.amble.ait.module.planet.core.space.planet.PlanetRenderInfo;
import dev.amble.ait.module.planet.core.space.planet.PlanetTransition;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlanetRegistry.class)
public class PlanetRegistryMixin {

    @Inject(method = "defaults", at = @At("TAIL"))
    private void addTitanPlanet(CallbackInfo ci) {
        PlanetRegistry self = (PlanetRegistry) (Object) this;

        // 泰坦维度 ID（必须与你的维度注册一致）
        Identifier titanId = new Identifier("doctor_m", "titan");

        // 土卫六现实参数：重力 0.14，无氧，不宜居，温度 94K（-179°C）
        Planet titan = new Planet(
                titanId,
                0.0688f,                     // 重力倍率（地球的14%）
                false,                     // 是否有氧气
                false,                     // 是否宜居
                94,                        // 温度（开尔文）
                PlanetRenderInfo.EMPTY,    // 渲染信息
                PlanetTransition.EMPTY     // 过渡信息
        );

        self.register(titan);
    }
}