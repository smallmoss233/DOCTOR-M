package doctor_m.module.sonic_plus.ModeGravitational;

import dev.amble.ait.core.item.sonic.SonicMode;
import dev.amble.ait.data.schema.sonic.SonicSchema;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GravitationalSwapMode extends SonicMode {
    public static final GravitationalSwapMode INSTANCE = new GravitationalSwapMode();

    private static final int WINDUP_TICKS = 30;
    private static final double PULL_SPEED = 0.15;
    private static final double INERTIA = 0.78;
    private static final double INERTIA_INV = 1.0 - INERTIA; // 0.22
    private static final double GRAVITY_OFFSET = 0.08;
    private static final double WINDUP_DRAG = 0.82;

    private GravitationalSwapMode() {
        super(2);
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.gravitational_swap")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
    }

    @Override
    public int maxTime() {
        return 72000;
    }

    @Override
    public Identifier model(SonicSchema.Models models) {
        return SonicMode.Modes.SCANNING.model(models);
    }

    @Override
    public int fuelCost() {
        return 1;
    }

    @Override
    public boolean startUsing(ItemStack stack, World world, PlayerEntity user, Hand hand) {
        return true;
    }

    @Override
    public void tick(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {
        if (!(user instanceof PlayerEntity player)) return;

        Vec3d center = player.getPos().add(0, player.getHeight() * 0.5, 0);

        // 客户端：前摇粒子
        if (world.isClient()) {
            if (ticks < WINDUP_TICKS && ticks % 4 == 0) {
                world.addParticle(ParticleTypes.END_ROD,
                        center.x, center.y, center.z,
                        0, 0.03, 0);
            }
            return;
        }

        Vec3d current = player.getVelocity();

        // 前摇阶段：减速蓄力
        if (ticks < WINDUP_TICKS) {
            player.setVelocity(current.multiply(WINDUP_DRAG));
            player.velocityDirty = true;
            player.fallDistance = 0;
            return;
        }

        // 正式拖拽
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d targetVel = look.multiply(PULL_SPEED);

        player.setVelocity(
                current.x * INERTIA + targetVel.x * INERTIA_INV,
                current.y * INERTIA + targetVel.y * INERTIA_INV + GRAVITY_OFFSET,
                current.z * INERTIA + targetVel.z * INERTIA_INV
        );
        player.velocityDirty = true;
        player.fallDistance = 0;
    }

    @Override
    public void stopUsing(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {}

    @Override
    public void finishUsing(ItemStack stack, World world, LivingEntity user) {}
}