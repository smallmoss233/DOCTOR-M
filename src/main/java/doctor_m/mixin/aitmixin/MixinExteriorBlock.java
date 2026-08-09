package doctor_m.mixin.aitmixin;

import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import dev.amble.ait.core.tardis.ServerTardis;
import doctor_m.util.TardisImpactFeedback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.blocks.ExteriorBlock;
import dev.amble.ait.core.entities.FallingTardisEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;

@Mixin(ExteriorBlock.class)
public class MixinExteriorBlock {

    @Shadow
    private Tardis findTardis(ServerWorld world, BlockPos pos) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean canFallThrough(World world, BlockPos pos) {
        throw new AssertionError();
    }

    /**
     * 恢复 ExteriorBlock 的 scheduledTick 下落检测。
     * 主分支中 scheduledTick 被完全注释掉了，导致：
     * - 邻居方块更新（敲掉下方方块）时不会自动下落
     * - 只有 AntiGravsControl 手动触发时才会下落
     *
     * getStateForNeighborUpdate / onBlockAdded / onLanding 都会 scheduleBlockTick，
     * 恢复后这些场景都能正常触发下落。
     */
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void aitmixin$restoreScheduledTick(BlockState state, ServerWorld world, BlockPos pos,
                                               Random random, CallbackInfo ci) {
        ci.cancel();

        Tardis tardis = this.findTardis(world, pos);
        if (tardis == null) return;

        // 只有着陆状态才检测下落
        if (tardis.travel().getState() != TravelHandlerBase.State.LANDED)
            return;

        // 下方是否可以坠落
        if (!canFallThrough(world, pos.down()))
            return;

        // 反重力开启时不坠落
        if (tardis.travel().antigravs().get())
            return;

        // 触发下落
        FallingTardisEntity.spawnFromBlock(world, pos, state);
    }
    
    /**
     * 检测实体高速撞击外壳，触发内部运动反馈。
     */
    @Inject(method = "onEntityCollision", at = @At("TAIL"))
    private void aitmixin$detectEntityImpact(net.minecraft.block.BlockState state, World world, BlockPos pos,
                                             Entity entity, CallbackInfo ci) {
        if (world.isClient()) return;
        if (!(world.getBlockEntity(pos) instanceof ExteriorBlockEntity exterior)) return;
        if (!exterior.isLinked() || exterior.tardis().isEmpty()) return;

        double speedSq = entity.getVelocity().lengthSquared();
        if (speedSq < 0.25) return; // 速度阈值

        ServerTardis tardis = exterior.tardis().get().asServer();
        float intensity = (float) Math.min(speedSq / 2.0, 0.6f);
        TardisImpactFeedback.apply(tardis, entity.getPos(), intensity);
    }
}