package client.render.entity; // 根据你的实际包名

import doctor_m.DOCTORM;
import doctor_m.DOCTORMClient;
import doctor_m.entities.data.entity_103_tardis;
import client.model.ModBipedModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class TardisRenderer extends MobEntityRenderer<entity_103_tardis, ModBipedModel<entity_103_tardis>> {
    public TardisRenderer(EntityRendererFactory.Context context) {
        super(context, new ModBipedModel<>(context.getPart(DOCTORMClient.TARDIS_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(entity_103_tardis entity) {
        String skin = entity.getSelectedSkin();
        if (skin == null || skin.isEmpty()) {
            // 如果获取失败，使用默认纹理避免崩溃
            return new Identifier(DOCTORM.MOD_ID, "textures/entity/tardis_skins/default.png");
        }
        return new Identifier(DOCTORM.MOD_ID, "textures/entity/tardis_skins/" + skin);
    }
}