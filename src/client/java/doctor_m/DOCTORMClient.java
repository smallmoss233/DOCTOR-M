package doctor_m;

import client.render.entity.EvereyeRenderer;
import client.render.entity.TardisRenderer;
import doctor_m.entities.entities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DOCTORMClient implements ClientModInitializer {

    // 手动定义玩家模型层（原版实际使用的 ID）
    public static final EntityModelLayer PLAYER_LAYER = new EntityModelLayer(new Identifier("minecraft", "player"), "main");
    public static final EntityModelLayer PLAYER_SLIM_LAYER = new EntityModelLayer(new Identifier("minecraft", "player_slim"), "main");

    @Override
    public void onInitializeClient() {

        // 注册实体渲染器
        EntityRendererRegistry.register(entities.TYPE_103_TARDIS, TardisRenderer::new);
        EntityRendererRegistry.register(entities.TYPE_103W_EVEREYE, EvereyeRenderer::new);
    }
}