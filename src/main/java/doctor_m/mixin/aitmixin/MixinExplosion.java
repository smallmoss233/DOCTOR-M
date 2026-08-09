package doctor_m.mixin.aitmixin;

import doctor_m.util.TardisImpactFeedback;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.AITBlocks;
import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import dev.amble.ait.core.entities.FallingTardisEntity;
import dev.amble.ait.core.tardis.ServerTardis;

@Mixin(Explosion.class)
public class MixinExplosion {

    @Shadow private World world;
    @Shadow private double x, y, z;
    @Shadow private float power;

    @Inject(method = "affectWorld", at = @At("TAIL"))
    private void aitmixin$detectTardisExplosion(boolean particles, CallbackInfo ci) {
        if (this.world.isClient()) return;

        Vec3d center = new Vec3d(this.x, this.y, this.z);
        double radius = this.power * 2.5;
        int minX = (int) (center.x - radius), minY = (int) (center.y - radius), minZ = (int) (center.z - radius);
        int maxX = (int) (center.x + radius), maxY = (int) (center.y + radius), maxZ = (int) (center.z + radius);

        // 扫描爆炸半径内的 ExteriorBlock
        for (BlockPos pos : BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (!this.world.getBlockState(pos).isOf(AITBlocks.EXTERIOR_BLOCK)) continue;
            if (!(this.world.getBlockEntity(pos) instanceof ExteriorBlockEntity exterior)) continue;
            if (!exterior.isLinked() || exterior.tardis().isEmpty()) continue;

            ServerTardis tardis = exterior.tardis().get().asServer();
            double dist = center.distanceTo(Vec3d.ofCenter(pos));
            float intensity = (float) ((1.0 - dist / radius) * (this.power / 4.0));
            TardisImpactFeedback.apply(tardis, Vec3d.ofCenter(pos), Math.max(0.1f, Math.min(intensity, 1.0f)));
        }

        // 扫描爆炸半径内的 FallingTardisEntity
        Box box = Box.of(center, radius * 2, radius * 2, radius * 2);
        for (FallingTardisEntity entity : this.world.getEntitiesByClass(FallingTardisEntity.class, box, e -> true)) {
            if (!entity.isLinked() || entity.tardis().isEmpty()) continue;

            ServerTardis tardis = entity.tardis().get().asServer();
            double dist = center.distanceTo(entity.getPos());
            float intensity = (float) ((1.0 - dist / radius) * (this.power / 4.0));
            TardisImpactFeedback.apply(tardis, entity.getPos(), Math.max(0.1f, Math.min(intensity, 1.0f)));
        }
    }
}