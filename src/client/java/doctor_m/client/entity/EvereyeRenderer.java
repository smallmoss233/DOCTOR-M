package doctor_m.client.entity;

import doctor_m.DOCTORM;
import doctor_m.DOCTORMClient;
import doctor_m.entities.data.Entity103wEvereye;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class EvereyeRenderer extends MobEntityRenderer<Entity103wEvereye, PlayerEntityModel<Entity103wEvereye>> {

    public EvereyeRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_SLIM_LAYER), true), 0.5f);
    }

    @Override
    public Identifier getTexture(Entity103wEvereye entity) {
        return new Identifier(DOCTORM.MOD_ID, "textures/entity/evereye.png");
    }
}