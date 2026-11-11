package doctor_m.mixin;

import dev.drtheo.multidim.MultiDim;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(MultiDim.class)
public class MultiDimMixin {

    @Redirect(
            method = "kickPlayers",

            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<PlayerEntity> safeIterator(List<PlayerEntity> originalList) {
        return new ArrayList<>(originalList).iterator();
    }
}