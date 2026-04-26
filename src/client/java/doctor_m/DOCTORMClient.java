package doctor_m;

import client.model.ModBipedModel;
import client.render.entity.TardisRenderer;
import client.render.entity.EvereyeRenderer;
import doctor_m.entities.entities;
import doctor_m.entities.data.entity_103_tardis;
import doctor_m.entities.data.entity_103w_evereye;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DOCTORMClient implements ClientModInitializer {

    // 定义模型层，用于区分不同实体的模型（即使模型相同，也建议分开注册）
    public static final EntityModelLayer TARDIS_LAYER = new EntityModelLayer(new Identifier(DOCTORM.MOD_ID, "tardis"), "main");
    public static final EntityModelLayer EVEREYE_LAYER = new EntityModelLayer(new Identifier(DOCTORM.MOD_ID, "evereye"), "main");

    @Override
    public void onInitializeClient() {
        // 注册模型层
        EntityModelLayerRegistry.registerModelLayer(TARDIS_LAYER, ModBipedModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(EVEREYE_LAYER, ModBipedModel::getTexturedModelData);

        // 注册实体渲染器
        EntityRendererRegistry.register(entities.TYPE_103_TARDIS, (EntityRendererFactory<entity_103_tardis>) TardisRenderer::new);
        EntityRendererRegistry.register(entities.TYPE_103W_EVEREYE, (EntityRendererFactory<entity_103w_evereye>) EvereyeRenderer::new);
    }
}