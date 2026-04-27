package client.render.entity;

import doctor_m.DOCTORM;
import doctor_m.DOCTORMClient;
import doctor_m.entities.data.entity_103w_evereye;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class EvereyeRenderer extends MobEntityRenderer<entity_103w_evereye, PlayerEntityModel<entity_103w_evereye>> {

    public EvereyeRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_SLIM_LAYER), true), 0.5f);
    }

    @Override
    public Identifier getTexture(entity_103w_evereye entity) {
        return new Identifier(DOCTORM.MOD_ID, "textures/entity/evereye_skin.png");
    }
}