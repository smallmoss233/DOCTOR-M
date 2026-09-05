package doctor_m;

import doctor_m.Item.data_item.DeMatGunItem;
import doctor_m.Item.data_item.KeytoTimeFragment.PocketWatchItem;
import doctor_m.Item.items;
import doctor_m.block.ModBlockEntities;
import doctor_m.block.data_block.EyeOfHarmonyObeliskBlock;
import doctor_m.client.Accessory.AccessoryKeyRegistry;
import doctor_m.client.Accessory.AccessoryPassiveButton;
import doctor_m.client.Accessory.handler.KeytoTimeKeyHandler;
import doctor_m.client.Accessory.handler.STCSKeyHandler;
import doctor_m.client.Shield.ForceFieldClientRenderer;
import doctor_m.client.Shield.ShieldNetworkingClient;
import doctor_m.client.Shield.ShieldOverlay;
import doctor_m.client.dimension.TitanDimensionEffects;
import doctor_m.client.entity.MarianJinRenderer;
import doctor_m.client.entity.Type103Renderer;
import doctor_m.client.gui.EyeOfHarmonyObeliskScreen;
import doctor_m.client.gui.PocketWatchHudOverlay;
import doctor_m.client.gui.VortexManipulatorScreen;
import doctor_m.client.network.AITMixinClientNetworking;
import doctor_m.client.network.DeMatGunClientNetwork;
import doctor_m.client.network.KeytoTimeTeleportClient;
import doctor_m.client.render.EyeOfHarmonyObeliskBlockEntityRenderer;
import doctor_m.client.render.ToyotaSpinningRotor.ToyotaSpinningRotorRenderer;
import doctor_m.client.render.VMTrinketRenderer;
import doctor_m.client.util.id.PlayerTitleCache;
import doctor_m.entities.Entities;
import doctor_m.network.INVERTSCREENPACKETNetwork;
import doctor_m.util.VMClientScreenOpener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

import static doctor_m.Item.items.FORCE_FIELD_SHIELD;
import static doctor_m.block.ModBlocks.*;

@Environment(EnvType.CLIENT)
public class DOCTORMClient implements ClientModInitializer {

    public static final EntityModelLayer PLAYER_LAYER = new EntityModelLayer(new Identifier("minecraft", "player"), "main");
    public static final EntityModelLayer PLAYER_SLIM_LAYER = new EntityModelLayer(new Identifier("minecraft", "player_slim"), "main");

    @Override
    public void onInitializeClient() {

        //按键系统
        AccessoryPassiveButton.register();
        AccessoryKeyRegistry.register(new STCSKeyHandler());
        AccessoryKeyRegistry.register(new KeytoTimeKeyHandler());

        ModelPredicateProviderRegistry.register(
                items.POCKET_WATCH,
                new Identifier("doctor_m", "open"),
                (stack, world, entity, seed) -> PocketWatchItem.isOpen(stack) ? 1.0f : 0.0f
        );

        HudRenderCallback.EVENT.register(new PocketWatchHudOverlay());

        // 泰坦维度效果
        DimensionRenderingRegistry.registerDimensionEffects(
                new Identifier("doctor_m", "titan"),
                new TitanDimensionEffects()
        );

        //和谐之眼
        BlockEntityRendererFactories.register(
                ModBlockEntities.EYE_OF_HARMONY_OBELISK,
                EyeOfHarmonyObeliskBlockEntityRenderer::new
        );

        EyeOfHarmonyObeliskBlock.OPEN_SCREEN_CALLBACK = obelisk -> {
            MinecraftClient.getInstance().setScreen(new EyeOfHarmonyObeliskScreen(obelisk));
        };

        KeytoTimeTeleportClient.register();
        ShieldNetworkingClient.register();
        ForceFieldClientRenderer.register();
        VMTrinketRenderer.register();
        HudRenderCallback.EVENT.register(new ShieldOverlay());
        AITMixinClientNetworking.init();
        PlayerTitleCache.register();

        EntityRendererRegistry.register(Entities.TYPE_103_TARDIS, Type103Renderer::new);
        EntityRendererRegistry.register(Entities.MARIAN_JIN, MarianJinRenderer::new);

        //玩偶不透明图层
        Block[] dollBlocks = {
                DOLL_JIN_MARY,
                DOLL_SMALLMOSS_OLD,
                DOLL_TC020,
                DOLL_ASDJDFK,
                DOLL_SIGEERTE,
                DOLL_TSINAFS_BCIM,
                DOLL_ASNIT_PNQING,
                DOLL_TIANX,
                DOLL_KILIN_MUS,
                DOLL_JOGGEST,
                DOLL_NX_SEEKER
        };

        for (Block block : dollBlocks) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            if (player == null) return;

            ItemStack stack = player.getMainHandStack();
            if (!(stack.getItem() instanceof DeMatGunItem gun)) return;
            if (!player.getItemCooldownManager().isCoolingDown(gun)
                    && client.options.attackKey.isPressed()) {

                boolean isAds = client.options.useKey.isPressed();
                DeMatGunClientNetwork.sendShootPacket(isAds);
            }
        });

        FabricModelPredicateProviderRegistry.register(
                FORCE_FIELD_SHIELD,
                new Identifier("blocking"),
                (stack, world, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0F : 0.0F
        );

        VMClientScreenOpener.opener = (player, stack) ->
                MinecraftClient.getInstance().setScreen(new VortexManipulatorScreen(player, stack));

        BlockEntityRendererRegistry.register(
                ModBlockEntities.TOYOTA_SPINNING_ROTOR,
                ToyotaSpinningRotorRenderer::new
        );

        //反色效果
        ClientPlayNetworking.registerGlobalReceiver(
                INVERTSCREENPACKETNetwork.INVERT_SCREEN_PACKET,
                (client, handler, buf, responseSender) -> {
                    int duration = buf.readInt();
                    client.execute(() -> {
                        GameRenderer gameRenderer = client.gameRenderer;

                        // 通过反射加载反色后处理着色器
                        try {
                            Method loadMethod = GameRenderer.class.getDeclaredMethod("loadPostProcessor", Identifier.class);
                            loadMethod.setAccessible(true);
                            loadMethod.invoke(gameRenderer, new Identifier("shaders/post/invert.json"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 延迟恢复
                        new Thread(() -> {
                            try {
                                Thread.sleep(duration * 50L); // 1 tick = 50ms
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            client.execute(() -> {
                                try {
                                    Method disableMethod = GameRenderer.class.getDeclaredMethod("disablePostProcessor");
                                    disableMethod.setAccessible(true);
                                    disableMethod.invoke(gameRenderer);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }).start();
                    });
                }
        );
    }
}