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

    private static final int WINDUP_TICKS = 30;        // 前摇
    private static final double PULL_SPEED = 0.15;   // 飞行速度
    private static final double INERTIA = 0.78;        // 保留 78% 原有速度

    private GravitationalSwapMode() {
        super(2); // 对应原 SCANNING
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.gravitational_swap")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
    }

    //飞行时间上限
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

        // 前摇阶段：不施加推力，只缓慢减速并蓄力
        if (ticks < WINDUP_TICKS) {
            Vec3d current = player.getVelocity();
            player.setVelocity(current.multiply(0.82));
            player.velocityModified = true;
            player.fallDistance = 0;
            return;
        }

        // 正式拖拽：大惯性 + 慢速
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d targetVel = look.multiply(PULL_SPEED);

        Vec3d current = player.getVelocity();

        player.setVelocity(
                current.x * INERTIA + targetVel.x * (1.0 - INERTIA),
                current.y * INERTIA + targetVel.y * (1.0 - INERTIA) + 0.08, // 抵消重力
                current.z * INERTIA + targetVel.z * (1.0 - INERTIA)
        );
        player.velocityModified = true;
        player.fallDistance = 0;
    }

    @Override
    public void stopUsing(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {}

    @Override
    public void finishUsing(ItemStack stack, World world, LivingEntity user) {}
}