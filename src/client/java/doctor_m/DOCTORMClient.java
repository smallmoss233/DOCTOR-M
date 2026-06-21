package doctor_m;

import client.render.entity.evereye_renderer;
import client.render.entity.tardis_renderer;
import doctor_m.entities.entities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DOCTORMClient implements ClientModInitializer {

    public static final EntityModelLayer PLAYER_LAYER = new EntityModelLayer(new Identifier("minecraft", "player"), "main");
    public static final EntityModelLayer PLAYER_SLIM_LAYER = new EntityModelLayer(new Identifier("minecraft", "player_slim"), "main");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(entities.TYPE_103_TARDIS, tardis_renderer::new);
        EntityRendererRegistry.register(entities.TYPE_103W_EVEREYE, evereye_renderer::new);
    }
}