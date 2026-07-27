package doctor_m.client.entity;

import doctor_m.DOCTORM;
import doctor_m.DOCTORMClient;
import doctor_m.entities.data.Entity103Tardis;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class Type103Renderer extends MobEntityRenderer<Entity103Tardis, PlayerEntityModel<Entity103Tardis>> {

    private final PlayerEntityModel<Entity103Tardis> defaultModel;
    private final PlayerEntityModel<Entity103Tardis> slimModel;

    public Type103Renderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_LAYER), false), 0.5f);
        this.defaultModel = new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_LAYER), false);
        this.slimModel = new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_SLIM_LAYER), true);
    }

    @Override
    public Identifier getTexture(Entity103Tardis entity) {
        String skin = entity.getSelectedSkin();
        if (skin == null || skin.isEmpty()) {
            return new Identifier(DOCTORM.MOD_ID, "textures/entity/tardis/default.png");
        }
        return new Identifier(DOCTORM.MOD_ID, "textures/entity/tardis/" + skin);
    }

    @Override
    public void render(Entity103Tardis entity, float yaw, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices,
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