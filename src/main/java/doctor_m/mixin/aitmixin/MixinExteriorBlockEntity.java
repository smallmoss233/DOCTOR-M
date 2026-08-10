package doctor_m.mixin.aitmixin;

import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExteriorBlockEntity.class)
public class MixinExteriorBlockEntity {

    /**
     * 修复 TARDIS 不会下落的根因之一：
     * 主分支每 tick 调用 scheduleBlockTick，不断重置 ExteriorBlock 的 scheduled tick，
     * 导致 scheduledTick 几乎从不触发，下落检测永远被推迟。
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aitmixin$cancelServerTick(World world, BlockPos pos, BlockState blockState,
                                           ExteriorBlockEntity blockEntity, CallbackInfo ci) {
        if (!world.isClient()) {
            ci.cancel();
        }
    }
}