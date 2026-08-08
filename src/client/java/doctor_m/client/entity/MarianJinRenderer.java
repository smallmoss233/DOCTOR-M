package doctor_m.client.entity;

import doctor_m.DOCTORM;
import doctor_m.DOCTORMClient;
import doctor_m.entities.data.Marian_Jin;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class MarianJinRenderer extends MobEntityRenderer<Marian_Jin, PlayerEntityModel<Marian_Jin>> {

    public MarianJinRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(DOCTORMClient.PLAYER_SLIM_LAYER), true), 0.5f);
    }

    @Override
    public Identifier getTexture(Marian_Jin entity) {
        return new Identifier(DOCTORM.MOD_ID, "textures/entity/evereye.png");
    }
}