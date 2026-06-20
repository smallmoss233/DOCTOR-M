package client.render.entity;

import doctor_m.DOCTORM;
import doctor_m.DOCTORMClient;

import doctor_m.entities.data.entity_103_tardis;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class tardis_renderer extends MobEntityRenderer<entity_103_tardis, PlayerEntityModel<entity_103_tardis>> {

    private final PlayerEntityModel<entity_103_tardis> defaultModel;
    private final PlayerEntityModel<entity_103_tardis> slimModel;

    public tardis_renderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_LAYER), false), 0.5f);
        this.defaultModel = new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_LAYER), false);
        this.slimModel = new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_SLIM_LAYER), true);
    }

    @Override
    public Identifier getTexture(entity_103_tardis entity) {
        String skin = entity.getSelectedSkin();
        if (skin == null || skin.isEmpty()) {
            return new Identifier(DOCTORM.MOD_ID, "textures/entity/tardis/default.png");
        }
        return new Identifier(DOCTORM.MOD_ID, "textures/entity/tardis/" + skin);
    }

    @Override
    public void render(entity_103_tardis entity, float yaw, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices,
                       net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light) {
        String modelType = entity.getModelType();
        if ("default".equals(modelType)) {
            this.model = defaultModel;
        } else {
            this.model = slimModel;
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}