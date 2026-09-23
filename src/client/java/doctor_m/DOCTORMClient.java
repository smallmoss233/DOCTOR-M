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
import doctor_m.client.module.ToyotaSpinningRotor.ToyotaSpinningRotorRenderer;
import doctor_m.client.render.Shield.ForceFieldClientRenderer;
import doctor_m.client.render.Shield.ShieldNetworkingClient;
import doctor_m.client.render.Shield.ShieldOverlay;
import doctor_m.client.dimension.TitanDimensionEffects;
import doctor_m.client.entity.MarianJinRenderer;
import doctor_m.client.entity.Type103Renderer;
import doctor_m.client.gui.EyeOfHarmonyObeliskScreen;
import doctor_m.client.gui.PocketWatchHudOverlay;
import doctor_m.client.gui.VortexManipulatorScreen;
import doctor_m.client.network.AITMixinClientNetworking;
import doctor_m.client.network.ConfigOpenHandler;
import doctor_m.client.network.DeMatGunClientNetwork;
import doctor_m.client.network.KeytoTimeTeleportClient;
import doctor_m.client.render.EmissiveBlockEntityRenderer;
import doctor_m.client.render.EyeOfHarmonyObeliskBlockEntityRenderer;
import doctor_m.client.render.TrinketRenderer.SCTrinketRenderer;
import doctor_m.client.render.TrinketRenderer.VMTrinketRenderer;
import doctor_m.client.util.id.PlayerTitleCache;
import doctor_m.entities.Entities;
import doctor_m.network.INVERTSCREENPACKETNetwork;
import doctor_m.util.VMClientScreenOpener;
import mosslib.client.bedrock.block.MossBedrockReloader;
import mosslib.client.bedrock.block.MossBedrockRenderer;
import mosslib.client.bedrock.entity.MossBedrockEntityRenderer;
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

    public static final EntityModelLayer PLAYER_LAYER =
            new EntityModelLayer(new Identifier("minecraft", "player"), "main");
    public static final EntityModelLayer PLAYER_SLIM_LAYER =
            new EntityModelLayer(new Identifier("minecraft", "player_slim"), "main");

    /** 需要以 cutout 层渲染的玩偶方块 */
    private static final Block[] DOLL_BLOCKS = {
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

    /** 反色效果的剩余 tick 数，由 client tick 递减 */
    private static int invertRemainingTicks = 0;
    private static boolean invertActive = false;

    @Override
    public void onInitializeClient() {
        registerKeybinds();
        registerItemModels();
        registerHudOverlays();
        registerDimensionEffects();
        registerBlockEntityRenderers();
        registerBlockRenderLayers();
        registerEntityRenderers();
        registerNetworking();
        registerClientTicks();
        registerScreenOpeners();
        ConfigOpenHandler.register();
        MossBedrockReloader.register();
        BlockEntityRendererFactories.register(
                ModBlockEntities.COFFEE_MACHINE,
                MossBedrockRenderer::new
        );
    }

    private void registerKeybinds() {
        AccessoryPassiveButton.register();
        AccessoryKeyRegistry.register(new STCSKeyHandler());
        AccessoryKeyRegistry.register(new KeytoTimeKeyHandler());
    }

    private void registerItemModels() {
        ModelPredicateProviderRegistry.register(
                items.POCKET_WATCH,
                new Identifier("doctor_m", "open"),
                (stack, world, entity, seed) -> PocketWatchItem.isOpen(stack) ? 1.0f : 0.0f
        );

        FabricModelPredicateProviderRegistry.register(
                FORCE_FIELD_SHIELD,
                new Identifier("blocking"),
                (stack, world, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getActiveItem() == stack
                                ? 1.0F : 0.0F
        );
    }

    private void registerHudOverlays() {
        HudRenderCallback.EVENT.register(new PocketWatchHudOverlay());
        HudRenderCallback.EVENT.register(new ShieldOverlay());
    }

    private void registerDimensionEffects() {
        DimensionRenderingRegistry.registerDimensionEffects(
                new Identifier("doctor_m", "titan"),
                new TitanDimensionEffects()
        );
    }

    //方块实体相关
    private void registerBlockEntityRenderers() {
        BlockEntityRendererFactories.register(
                ModBlockEntities.EYE_OF_HARMONY_OBELISK,
                EyeOfHarmonyObeliskBlockEntityRenderer::new
        );

        BlockEntityRendererRegistry.register(
                ModBlockEntities.OXYGEN_CHARGER_ENTITY,
                EmissiveBlockEntityRenderer::new
        );

        BlockEntityRendererRegistry.register(
                ModBlockEntities.UNDERWATER_OXYGEN_GENERATOR_ENTITY,
                EmissiveBlockEntityRenderer::new
        );

        BlockEntityRendererRegistry.register(
                ModBlockEntities.TOYOTA_SPINNING_ROTOR,
                ToyotaSpinningRotorRenderer::new
        );
    }

    private void registerBlockRenderLayers() {
        for (Block block : DOLL_BLOCKS) {
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
        }
    }

    private void registerEntityRenderers() {
        EntityRendererRegistry.register(Entities.TYPE_103_TARDIS, Type103Renderer::new);
        EntityRendererRegistry.register(Entities.MARIAN_JIN, MarianJinRenderer::new);  //K动画后记得换成MossBedrockEntityRenderer
    }

    private void registerNetworking() {
        KeytoTimeTeleportClient.register();
        ShieldNetworkingClient.register();
        ForceFieldClientRenderer.register();
        VMTrinketRenderer.register();
        SCTrinketRenderer.register();
        AITMixinClientNetworking.init();
        PlayerTitleCache.register();

        ClientPlayNetworking.registerGlobalReceiver(
                INVERTSCREENPACKETNetwork.INVERT_SCREEN_PACKET,
                (client, handler, buf, responseSender) -> {
                    int duration = buf.readInt();
                    client.execute(() -> startInvertEffect(client, duration));
                }
        );
    }

    private void registerClientTicks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickInvertEffect(client);

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
    }

    private void registerScreenOpeners() {
        EyeOfHarmonyObeliskBlock.OPEN_SCREEN_CALLBACK = obelisk ->
                MinecraftClient.getInstance().setScreen(new EyeOfHarmonyObeliskScreen(obelisk));

        VMClientScreenOpener.opener = (player, stack) ->
                MinecraftClient.getInstance().setScreen(new VortexManipulatorScreen(player, stack));
    }

    // ==================== 反色效果 ====================

    /** 启动反色效果，持续 duration 个 tick。 */
    private static void startInvertEffect(MinecraftClient client, int durationTicks) {
        GameRenderer gameRenderer = client.gameRenderer;

        try {
            Method loadMethod = GameRenderer.class.getDeclaredMethod(
                    "loadPostProcessor", Identifier.class);
            loadMethod.setAccessible(true);
            loadMethod.invoke(gameRenderer, new Identifier("shaders/post/invert.json"));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        invertRemainingTicks = Math.max(1, durationTicks);
        invertActive = true;
    }

    /** 每个客户端 tick 递减倒计时，归零时关闭反色效果。 */
    private static void tickInvertEffect(MinecraftClient client) {
        if (!invertActive) return;
        if (--invertRemainingTicks > 0) return;

        invertActive = false;

        try {
            Method disableMethod = GameRenderer.class.getDeclaredMethod("disablePostProcessor");
            disableMethod.setAccessible(true);
            disableMethod.invoke(client.gameRenderer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}