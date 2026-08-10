package doctor_m.mixin.ait_oxygenatedmixin;

import doctor_m.module.space_plus.VacuumEatingHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntityEat {

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof ServerPlayerEntity player) {
            VacuumEatingHandler.onPlayerActuallyEat(player);
        }
    }
}