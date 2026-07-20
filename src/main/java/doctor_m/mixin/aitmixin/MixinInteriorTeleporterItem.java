package doctor_m.mixin.aitmixin;

import dev.amble.ait.core.item.InteriorTeleporterItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InteriorTeleporterItem.class)
public class MixinInteriorTeleporterItem {

    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;setCount(I)V"
            )
    )
    private void replaceConsumptionWithDurability(
            ItemStack stack, int count,
            World world, PlayerEntity user, Hand hand
    ) {
        if (count == stack.getCount() - 1) {
            stack.damage(1, user, p -> {});
        } else {
            stack.setCount(count);
        }
    }
}