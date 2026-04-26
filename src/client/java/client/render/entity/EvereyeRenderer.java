package client.render.entity;

import doctor_m.DOCTORM;
import doctor_m.DOCTORMClient;
import doctor_m.entities.data.entity_103w_evereye;
import client.model.ModBipedModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class EvereyeRenderer extends MobEntityRenderer<entity_103w_evereye, ModBipedModel<entity_103w_evereye>> {
    private static final Identifier FIXED_SKIN = new Identifier(DOCTORM.MOD_ID, "textures/entity/evereye_skin.png");

    public EvereyeRenderer(EntityRendererFactory.Context context) {
        super(context, new ModBipedModel<>(context.getPart(DOCTORMClient.EVEREYE_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(entity_103w_evereye entity) {
        return FIXED_SKIN;
    }
}