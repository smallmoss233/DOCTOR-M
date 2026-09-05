package doctor_m.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class DOCTORMMRenderLayers extends RenderLayer {

    private static final Function<Identifier, RenderLayer> EMISSIVE_UNSORTED =
            Util.memoize(id -> emissiveInternal(id, false));
    private static final Function<Identifier, RenderLayer> EMISSIVE_SORTED =
            Util.memoize(id -> emissiveInternal(id, true));
    private static final Function<Identifier, RenderLayer> ADDITIVE_EMISSIVE =
            Util.memoize(DOCTORMMRenderLayers::additiveEmissiveInternal);

    private DOCTORMMRenderLayers(String name, VertexFormat format, VertexFormat.DrawMode drawMode,
                                int expectedSize, boolean crumbling, boolean translucent,
                                Runnable startAction, Runnable endAction) {
        super(name, format, drawMode, expectedSize, crumbling, translucent, startAction, endAction);
    }

    private static RenderLayer emissiveInternal(Identifier texture, boolean sorted) {
        RenderPhase.Texture texturePhase = new RenderPhase.Texture(texture, false, false);
        MultiPhaseParameters params = MultiPhaseParameters.builder()
                .program(RenderPhase.EYES_PROGRAM)
                .texture(texturePhase)
                .cull(DISABLE_CULLING)
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .lightmap(ENABLE_LIGHTMAP)
                .writeMaskState(COLOR_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .build(false);
        return RenderLayer.of(
                sorted ? "doctor_m:emissive_sorted" : "doctor_m:emissive_unsorted",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256, false, sorted, params);
    }

    private static RenderLayer additiveEmissiveInternal(Identifier texture) {
        MultiPhaseParameters params = MultiPhaseParameters.builder()
                .program(RenderPhase.EYES_PROGRAM)
                .texture(new RenderPhase.Texture(texture, false, false))
                .transparency(RenderPhase.ADDITIVE_TRANSPARENCY)
                .cull(DISABLE_CULLING)
                .writeMaskState(COLOR_MASK)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .build(false);
        return RenderLayer.of(
                "doctor_m:additive_emissive",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256, false, true, params);
    }

    /** 标准透明发光层，不排序 */
    public static RenderLayer emissive(Identifier texture) {
        return EMISSIVE_UNSORTED.apply(texture);
    }

    /** 排序的透明发光层，用于需要正确绘制顺序的情况 */
    public static RenderLayer emissiveSorted(Identifier texture) {
        return EMISSIVE_SORTED.apply(texture);
    }

    /** 加法混合发光层，发光更强，但会叠加颜色，适合光晕效果 */
    public static RenderLayer additiveEmissive(Identifier texture) {
        return ADDITIVE_EMISSIVE.apply(texture);
    }

    public static RenderLayer tardisEmissiveCullZOffset(Identifier texture) {
        return emissive(texture);
    }

    public static RenderLayer tardisEmissiveCullZOffsetSorted(Identifier texture) {
        return emissiveSorted(texture);
    }
}