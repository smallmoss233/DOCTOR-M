package doctor_m.client.render.ToyotaSpinningRotor;

import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import doctor_m.DOCTORM;
import doctor_m.block.entities.ToyotaSpinningRotorBlockEntity;
import doctor_m.block.entities.ToyotaSpinningRotorBlockEntityClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import dev.amble.ait.core.tardis.Tardis;

public class ToyotaSpinningRotorRenderer<T extends ToyotaSpinningRotorBlockEntity>
        implements BlockEntityRenderer<T> {

    public static final Identifier TEXTURE = new Identifier(DOCTORM.MOD_ID,
            "textures/block/toyota_spinning_rotor.png");

    public static final Identifier EMISSIVE_TEXTURE = new Identifier(DOCTORM.MOD_ID,
            "textures/block/toyota_spinning_rotor_emission.png");

    public ToyotaSpinningRotorRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(ToyotaSpinningRotorBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        // 默认值：未链接时静止
        boolean hasPower = false;
        TravelHandlerBase.State state = TravelHandlerBase.State.LANDED;

        if (entity.isLinked()) {
            var ref = entity.tardis();
            if (ref.isPresent()) {
                Tardis tardis = ref.get();
                hasPower = tardis.fuel().hasPower();
                state = entity.displayState != null
                        ? entity.displayState
                        : tardis.travel().getState();
            }
        }

        ToyotaSpinningRotorModel model = new ToyotaSpinningRotorModel(
                ToyotaSpinningRotorModel.getTexturedModelData().createModel());
        model.animateBlockEntity(entity, state, hasPower);

        matrices.push();
        matrices.scale(1f, 1f, 1f);
        matrices.translate(0.5f, 1.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));

        model.renderWithAnimations(entity, model.getPart(), matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)),
                light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        model.renderWithAnimations(entity, model.getPart(), matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCullZOffset(EMISSIVE_TEXTURE)),
                0xf000f0, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        matrices.pop();

        // 只有链接了才播声音
        if (entity.isLinked()) {
            ToyotaSpinningRotorBlockEntityClient.tick(entity, state);
        }
    }
}