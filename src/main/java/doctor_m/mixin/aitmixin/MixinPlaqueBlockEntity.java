package doctor_m.mixin.aitmixin;

import dev.amble.ait.api.tardis.link.v2.TardisRef;
import dev.amble.ait.core.blockentities.PlaqueBlockEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.TardisDesktop;
import doctor_m.util.type.TardisTypeMapper;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlaqueBlockEntity.class)
public abstract class MixinPlaqueBlockEntity {

    @Inject(method = "getPlaqueText", at = @At("HEAD"), cancellable = true)
    private void overridePlaqueText(CallbackInfoReturnable<String> cir) {
        PlaqueBlockEntity self = (PlaqueBlockEntity) (Object) this;
        TardisRef ref = self.tardis();

        if (ref == null) return;

        Tardis tardis = ref.get();
        if (tardis == null) return;

        // 获取当前内饰 ID
        TardisDesktop desktop = tardis.getDesktop();
        if (desktop == null) return;

        Identifier desktopId = desktop.getSchema().id();
        if (desktopId == null) return;

        // 从映射表中查找型号
        String modelType = TardisTypeMapper.getTypeForDesktop(desktopId);
        cir.setReturnValue(modelType);
    }
}