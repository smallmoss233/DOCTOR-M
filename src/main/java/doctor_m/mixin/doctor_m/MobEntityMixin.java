package doctor_m.mixin.doctor_m;

import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.data_itme.KeytoTimeItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MobEntity.class)
public class MobEntityMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity player) {
            var trinketComp = TrinketsApi.getTrinketComponent(player).orElse(null);
            if (trinketComp != null) {
                Optional<ItemStack> timeKeyStack = trinketComp.getEquipped(stack -> stack.getItem() instanceof KeytoTimeItem)
                        .stream().findFirst().map(Pair::getRight);
                if (timeKeyStack.isPresent()) {
                    NbtCompound nbt = timeKeyStack.get().getNbt();
                    boolean neutralEnabled = nbt != null && nbt.getBoolean("neutral_mode");
                    if (neutralEnabled) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}