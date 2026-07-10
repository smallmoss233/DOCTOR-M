package doctor_m.mixin.client.ait;

import doctor_m.util.config.ConfigManager;
import net.minecraft.client.model.*;
import dev.amble.ait.client.models.machines.ShieldsModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ShieldsModel.class)
public class MixinShieldsModel {

    /**
     * 根据配置的半边长动态生成护盾模型尺寸
     * 模型尺寸 = 2 * 半边长 * 16（因为原模型 128 对应半边长 4，比例系数 16）
     */
    @Overwrite
    public static TexturedModelData getTexturedModelData() {
        double halfSize = ConfigManager.getConfig().shieldHalfSize;
        int modelSize = (int) (2 * halfSize * 16); // 例如 halfSize=4 → 128

        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData shields = modelPartData.addChild(
                "shields",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-modelSize / 2.0F, -modelSize / 2.0F, -modelSize / 2.0F,
                                modelSize, modelSize, modelSize, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F)
        );
        return TexturedModelData.of(modelData, 64, 64);
    }
}