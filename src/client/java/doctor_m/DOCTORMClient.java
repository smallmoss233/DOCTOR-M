package doctor_m;

import doctor_m.client.TitanDimensionEffects;
import doctor_m.client.entity.evereye_renderer;
import doctor_m.client.entity.tardis_renderer;
import doctor_m.client.Shield.ShieldNetworkingClient;
import doctor_m.client.Shield.ShieldOverlay;
import doctor_m.entities.entities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DOCTORMClient implements ClientModInitializer {

    public static final EntityModelLayer PLAYER_LAYER = new EntityModelLayer(new Identifier("minecraft", "player"), "main");
    public static final EntityModelLayer PLAYER_SLIM_LAYER = new EntityModelLayer(new Identifier("minecraft", "player_slim"), "main");

    @Override
    public void onInitializeClient() {
        // 泰坦维度效果
        DimensionRenderingRegistry.registerDimensionEffects(
                new Identifier("doctor_m", "titan"),
                new TitanDimensionEffects()
        );

        ShieldNetworkingClient.register();
        HudRenderCallback.EVENT.register(new ShieldOverlay());

        EntityRendererRegistry.register(entities.TYPE_103_TARDIS, tardis_renderer::new);
        EntityRendererRegistry.register(entities.TYPE_103W_EVEREYE, evereye_renderer::new);
    }
}