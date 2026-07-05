package doctor_m.mixin.client.ait;

import net.minecraft.client.model.*;
import dev.amble.ait.client.models.machines.ShieldsModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ShieldsModel.class)
public class MixinShieldsModel {

    /**
     * 将护盾模型尺寸从 64 扩大到 128
     */
    @Overwrite
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData shields = modelPartData.addChild(
                "shields",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-64.0F, -64.0F, -64.0F, 128.0F, 128.0F, 128.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F)
        );
        return TexturedModelData.of(modelData, 64, 64);
    }
}