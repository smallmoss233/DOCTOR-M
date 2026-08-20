package doctor_m.client.render.ToyotaSpinningRotor;

import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import doctor_m.DOCTORM;
import doctor_m.block.data_block.ToyotaSpinningRotorBlock;
import doctor_m.block.entities.ToyotaSpinningRotorBlockEntity;
import doctor_m.block.entities.ToyotaSpinningRotorBlockEntityClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class ToyotaSpinningRotorRenderer<T extends ToyotaSpinningRotorBlockEntity>
        implements BlockEntityRenderer<T> {

    public static final Identifier[] TEXTURES = new Identifier[]{
            new Identifier(DOCTORM.MOD_ID, "textures/block/toyota_spinning_rotor.png"),
            new Identifier(DOCTORM.MOD_ID, "textures/block/spinnything_aperture.png"),
            new Identifier(DOCTORM.MOD_ID, "textures/block/spinnything_legacy.png"),
    };

    public static final Identifier[] EMISSIVE_TEXTURES = new Identifier[]{
            new Identifier(DOCTORM.MOD_ID, "textures/block/toyota_spinning_rotor_emission.png"),
            null,
            new Identifier(DOCTORM.MOD_ID, "textures/block/spinnything_legacy_emission.png"),
    };

    public ToyotaSpinningRotorRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(ToyotaSpinningRotorBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

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

        int variant = entity.getCachedState().get(ToyotaSpinningRotorBlock.VARIANT);
        Identifier texture = TEXTURES[variant % TEXTURES.length];
        Identifier emissive = EMISSIVE_TEXTURES[variant % EMISSIVE_TEXTURES.length];

        ToyotaSpinningRotorModel model = new ToyotaSpinningRotorModel(
                ToyotaSpinningRotorModel.getTexturedModelData().createModel());
        model.animateBlockEntity(entity, state, hasPower);

        matrices.push();
        matrices.scale(1f, 1f, 1f);
        matrices.translate(0.5f, 1.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));

        model.renderWithAnimations(entity, model.getPart(), matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture)),
                light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        if (emissive != null) {
            model.renderWithAnimations(entity, model.getPart(), matrices,
                    vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCullZOffset(emissive)),
                    0xf000f0, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        matrices.pop();

        if (entity.isLinked()) {
            ToyotaSpinningRotorBlockEntityClient.tick(entity, state);
        }
    }
}