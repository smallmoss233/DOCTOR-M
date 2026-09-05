package doctor_m.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Set;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class EyeOfHarmonyRenderLayers extends RenderLayer {

    private static RenderLayer emissive(Identifier texture, boolean sorted) {
        RenderPhase.Texture texture2 = new RenderPhase.Texture(texture, false, false);
        MultiPhaseParameters multiPhaseParameters = RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.EYES_PROGRAM)
                .texture(texture2)
                .cull(DISABLE_CULLING)
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .lightmap(ENABLE_LIGHTMAP)
                .writeMaskState(COLOR_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .build(false);
        return RenderLayer.of(sorted ? "emissive_cull_z_offset_sorted" : "emissive_cull_z_offset_unsorted",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256,
                false, sorted, multiPhaseParameters);
    }

    private static final Set<Identifier> SORT_SENSITIVE_EMISSION = Set.of(
            new Identifier("doctor_m", "textures/environment/eye_of_harmony.png") // 示例，可改为你自己的纹理
    );

    private static final Function<Identifier, RenderLayer> EMISSIVE_SORTED = Util.memoize(texture -> emissive(texture, true));
    private static final Function<Identifier, RenderLayer> EMISSIVE_UNSORTED = Util.memoize(texture -> emissive(texture, false));

    public static RenderLayer tardisEmissiveCullZOffset(Identifier texture) {
        return SORT_SENSITIVE_EMISSION.contains(texture)
                ? EMISSIVE_SORTED.apply(texture)
                : EMISSIVE_UNSORTED.apply(texture);
    }

    public static RenderLayer tardisEmissiveCullZOffsetSorted(Identifier texture) {
        return EMISSIVE_SORTED.apply(texture);
    }

    private EyeOfHarmonyRenderLayers(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode,
                                     int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction,
                                     Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }
}