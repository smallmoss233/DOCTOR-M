package doctor_m.mixin.doctor_m;

import dev.amble.ait.core.blockentities.ConsoleBlockEntity;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.control.Control;
import dev.amble.ait.core.tardis.control.impl.SonicPortControl;
import doctor_m.Item.data_itme.TracerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SonicPortControl.class)
public abstract class SonicPortControlMixin {

    @Inject(method = "runServer", at = @At("HEAD"), cancellable = true)
    private void doctor_m$insertTracer(Tardis tardis, ServerPlayerEntity player, ServerWorld world,
                                       BlockPos console, boolean leftClick,
                                       CallbackInfoReturnable<Control.Result> cir) {

        ItemStack held = player.getMainHandStack();

        // 不是追踪器就交给原逻辑
        if (!(held.getItem() instanceof TracerItem)) {
            return;
        }

        if (!(world.getBlockEntity(console) instanceof ConsoleBlockEntity consoleBe)) {
            return;
        }

        boolean hasSonicStored = !consoleBe.getSonicScrewdriver().isEmpty();
        boolean hasHandlesStored = tardis.butler().getHandles() != null;

        // 取出操作交给原逻辑（TracerItem 存在 sonicScrewdriver 字段里，原逻辑取出分支通用）
        if ((leftClick || player.isSneaking()) && (hasSonicStored || hasHandlesStored)) {
            return;
        }

        // 插入追踪器
        if (!hasSonicStored && !hasHandlesStored) {
            consoleBe.setSonicScrewdriver(held.copy());
            player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);

            world.playSound(null, player.getBlockPos(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);

            cir.setReturnValue(Control.Result.SUCCESS);
        } else {
            // 端口已被占用
            cir.setReturnValue(Control.Result.FAILURE);
        }
    }
}