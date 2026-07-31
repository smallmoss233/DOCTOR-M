package doctor_m.module.sonic_plus.ModeChargedZeiton;

import dev.amble.ait.core.item.sonic.SonicMode;
import dev.amble.ait.data.schema.sonic.SonicSchema;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LaserMode extends SonicMode {
    public static final LaserMode INSTANCE = new LaserMode();

    private static final double RANGE = 48.0;
    private static final float DAMAGE = 6.0f;

    private LaserMode() {
        super(1);
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.laser")
                .formatted(Formatting.RED, Formatting.BOLD);
    }

    @Override
    public int maxTime() {
        return 72000;
    }

    @Override
    public Identifier model(SonicSchema.Models models) {
        return SonicMode.Modes.OVERLOAD.model(models);
    }

    @Override
    public int fuelCost() {
        return 8;
    }

    @Override
    public boolean startUsing(ItemStack stack, World world, PlayerEntity user, Hand hand) {
        return true;
    }

    @Override
    public void tick(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {
        if (!(user instanceof PlayerEntity player)) return;

        Vec3d eyePos = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F);

        if (world.isClient()) {
            Vec3d end = eyePos.add(look.multiply(RANGE));
            spawnBeamParticles(world, eyePos, end, ParticleTypes.END_ROD, 0.6);
        } else {
            HitResult hit = getHitResult(player, RANGE);
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();
                target.damage(world.getDamageSources().magic(), DAMAGE);
            }
        }
    }

    @Override
    public void stopUsing(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {}

    @Override
    public void finishUsing(ItemStack stack, World world, LivingEntity user) {}

    private static void spawnBeamParticles(World world, Vec3d start, Vec3d end, ParticleEffect particle, double step) {
        Vec3d dir = end.subtract(start);
        double len = dir.length();
        dir = dir.normalize();

        for (double d = 0; d < len; d += step) {
            Vec3d pos = start.add(dir.multiply(d));
            world.addParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }
}