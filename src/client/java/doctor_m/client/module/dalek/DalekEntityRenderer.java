package doctor_m.client.module.dalek;

import doctor_m.module.dalek.DalekEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class DalekEntityRenderer<T extends DalekEntity> extends MobEntityRenderer<T, DalekModel<T>> {

    private Identifier texture;

    public DalekEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new DalekModel<>(DalekModel.getTexturedModelData().createModel()), 0.5f);
        this.addFeature(new DalekLightFeatureRenderer<>(this));
    }

    public void updateDalek(DalekEntity dalekEntity) {
        Identifier newTexture = dalekEntity.getDalek().texture();
        if (this.texture != newTexture) {
            this.texture = newTexture;
        }
    }

    @Override
    public Identifier getTexture(DalekEntity entity) {
        this.updateDalek(entity);
        return this.texture;
    }
}
