package doctor_m.mixin.aitmixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.amble.ait.core.tardis.handler.travel.ProgressiveTravelHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ProgressiveTravelHandler.class)
public class ProgressiveTravelHandlerMixin {

    @WrapMethod(method = "recalculate")
    private void preventCrash(Operation<Void> original) {
        try {
            original.call();
        } catch (ArithmeticException ignored) {
        }
    }
}