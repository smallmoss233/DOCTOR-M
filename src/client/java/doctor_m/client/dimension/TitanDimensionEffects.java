package doctor_m.client.dimension;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class TitanDimensionEffects extends DimensionEffects {

    public TitanDimensionEffects() {
        // SkyType.NONE → 不绘制天体，只有雾色作为背景
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
        // 固定为黄昏橙红色，不受太阳高度影响
        return new Vec3d(0.9, 0.5, 0.2);
    }

    @Override
    public boolean useThickFog(int camX, int camY) {
        return true;   // 浓雾，符合厚大气
    }

    @Override
    public float getCloudsHeight() {
        return 160.0f; // 云层调高，增加层次感
    }
}