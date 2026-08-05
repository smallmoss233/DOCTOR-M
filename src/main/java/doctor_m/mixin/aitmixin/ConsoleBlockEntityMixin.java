package doctor_m.mixin.aitmixin;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.item.SonicItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConsoleBlockEntity.class)
public class ConsoleBlockEntityMixin {

    /**
     * 控制台给音速起子充能时，用起子自己的 getMaxFuel（受模块影响），
     * 而不是被 ConsoleBlockEntity 的固定 1000 上限锁死。
     */
    @Inject(method = "getMaxFuel", at = @At("RETURN"), cancellable = true)
    private void doctor_m$sonicMaxFuel(ItemStack stack, CallbackInfoReturnable<Double> cir) {
        if (stack.getItem() instanceof SonicItem sonicItem) {
            cir.setReturnValue(sonicItem.getMaxFuel(stack));
        }
    }
}