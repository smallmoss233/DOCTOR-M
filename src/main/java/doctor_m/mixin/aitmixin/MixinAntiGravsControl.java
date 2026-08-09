package doctor_m.mixin.aitmixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.amble.ait.core.AITBlocks;
import dev.amble.ait.core.entities.FallingTardisEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.impl.AntiGravsControl;
import dev.amble.lib.data.CachedDirectedGlobalPos;

@Mixin(AntiGravsControl.class)
public class MixinAntiGravsControl {

    /**
     * 关闭反重力时立即尝试触发下落，不等待 scheduled tick（2 tick 延迟）。
     * 修复分支使用 ExteriorBlock.tryTriggerFall()，主分支没有这个方法，
     * 这里直接内联实现。
     */
    @Inject(method = "runServer", at = @At("RETURN"))
    private void aitmixin$triggerFallWhenDisabled(Tardis tardis, ServerPlayerEntity player, ServerWorld world,
                                                  BlockPos console, boolean leftClick,
                                                  CallbackInfoReturnable<Control.Result> cir) {
        // 反重力仍处于开启状态，不需要下落
        if (tardis.travel().antigravs().get()) return;

        CachedDirectedGlobalPos globalPos = tardis.travel().position();
        ServerWorld targetWorld = globalPos.getWorld();
        BlockPos pos = globalPos.getPos();

        BlockState state = targetWorld.getBlockState(pos);
        if (!state.isOf(AITBlocks.EXTERIOR_BLOCK)) return;

        // 检查下方是否可以坠落
        BlockPos down = pos.down();
        if (!aitmixin$canFallThrough(targetWorld.getBlockState(down))) return;

        // 立即生成 FallingTardisEntity
        FallingTardisEntity.spawnFromBlock(targetWorld, pos, state);
    }

    @Unique
    private static boolean aitmixin$canFallThrough(BlockState state) {
        return state.isAir()
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || !state.getFluidState().isEmpty()
                || state.isReplaceable();
    }
}