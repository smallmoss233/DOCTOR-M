package doctor_m.mixin.aitmixin;

import dev.amble.ait.api.tardis.link.v2.TardisRef;
import dev.amble.ait.core.blockentities.PlaqueBlockEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.TardisDesktop;
import doctor_m.util.type.TardisTypeMapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlaqueBlockEntity.class)
public abstract class MixinPlaqueBlockEntity {

    // 新版：getPlaqueText() 返回 Text
    @Inject(
            method = "getPlaqueText()Lnet/minecraft/text/Text;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            expect = 0
    )
    private void doctor_m$overridePlaqueText_Text(CallbackInfoReturnable<Text> cir) {
        Text result = doctor_m$computeOverride();
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    // 旧版：getPlaqueText() 返回 String
    @Inject(
            method = "getPlaqueText()Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            expect = 0
    )
    private void doctor_m$overridePlaqueText_String(CallbackInfoReturnable<String> cir) {
        Text result = doctor_m$computeOverride();
        if (result != null) {
            cir.setReturnValue(result.getString());
        }
    }

    /**
     * 共享取值逻辑。返回 null 表示不干预，交回原版。
     */
    private Text doctor_m$computeOverride() {
        PlaqueBlockEntity self = (PlaqueBlockEntity) (Object) this;
        TardisRef ref = self.tardis();
        if (ref == null) return null;

        Tardis tardis = ref.get();
        if (tardis == null) return null;

        TardisDesktop desktop = tardis.getDesktop();
        if (desktop == null) return null;

        Identifier desktopId = desktop.getSchema().id();
        if (desktopId == null) return null;

        String modelType = TardisTypeMapper.getTypeForDesktop(desktopId);
        if (modelType != null && !modelType.isEmpty()) {
            return Text.literal(modelType);
        }
        return null;
    }
}