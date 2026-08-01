package doctor_m.module.sonic_plus.ModeGravitational;

import java.util.List;

import dev.amble.ait.core.item.sonic.SonicMode;
import dev.amble.ait.data.schema.sonic.SonicSchema;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GravitationalShieldingMode extends SonicMode {
    public static final GravitationalShieldingMode INSTANCE = new GravitationalShieldingMode();

    private static final double RADIUS = 3.0;
    private static final double RADIUS_SQ = RADIUS * RADIUS;
    private static final double PUSH_STRENGTH = 0.6;
    private static final double INERTIA_H = 0.8;   // 水平惯性保留
    private static final double INERTIA_V = 0.5;   // 垂直惯性保留
    private static final double PUSH_V_SCALE = 0.8; // 垂直推力衰减（避免无限升空）

    private GravitationalShieldingMode() {
        super(1);
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.gravitational_shielding")
                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
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
        return 1;
    }

    @Override
    public boolean startUsing(ItemStack stack, World world, PlayerEntity user, Hand hand) {
        return true;
    }

    @Override
    public void tick(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {
        if (world.isClient()) return;
        if (!(user instanceof PlayerEntity player)) return;

        Vec3d center = player.getPos();
        // 优化：Box.of 语义更清晰，减少一次对象分配
        Box box = Box.of(center, RADIUS * 2, RADIUS * 2, RADIUS * 2);

        List<Entity> entities = world.getOtherEntities(player, box, entity ->
                entity.isAlive() && !entity.isSpectator()
        );

        Vec3d playerPos = player.getPos();

        for (Entity entity : entities) {
            if (entity.squaredDistanceTo(player) > RADIUS_SQ) continue;

            Vec3d dir = entity.getPos().subtract(playerPos);
            double lenSq = dir.lengthSquared();

            // 零向量保护 + 贴脸时默认向上推
            if (lenSq < 0.001) {
                dir = new Vec3d(0, 1, 0);
            } else {
                dir = dir.normalize();
            }

            Vec3d current = entity.getVelocity();
            Vec3d push = dir.multiply(PUSH_STRENGTH);

            entity.setVelocity(
                    current.x * INERTIA_H + push.x,
                    current.y * INERTIA_V + push.y * PUSH_V_SCALE,
                    current.z * INERTIA_H + push.z
            );
            entity.velocityDirty = true; // 1.20.1 Yarn 正确字段名
            entity.fallDistance = 0;
        }
    }

    @Override
    public void stopUsing(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {}

    @Override
    public void finishUsing(ItemStack stack, World world, LivingEntity user) {}
}