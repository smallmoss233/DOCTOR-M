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
    private static final double PUSH_STRENGTH = 0.6;

    private GravitationalShieldingMode() {
        super(1); // 对应原 OVERLOAD
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.gravitational_shielding")
                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
    }

    //护盾时间上限
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
        Box box = new Box(
                center.x - RADIUS, center.y - RADIUS, center.z - RADIUS,
                center.x + RADIUS, center.y + RADIUS, center.z + RADIUS
        );

        List<Entity> entities = world.getOtherEntities(player, box, entity ->
                entity.isAlive() && !entity.isSpectator()
        );

        for (Entity entity : entities) {
            if (entity.squaredDistanceTo(player) > RADIUS * RADIUS) continue;

            Vec3d dir = entity.getPos().subtract(player.getPos());
            if (dir.lengthSquared() < 0.001) {
                dir = new Vec3d(0, 1, 0);
            } else {
                dir = dir.normalize();
            }

            Vec3d current = entity.getVelocity();
            Vec3d push = dir.multiply(PUSH_STRENGTH);

            // 保留一点原有惯性，叠加推力
            entity.setVelocity(
                    current.x * 0.3 + push.x,
                    current.y * 0.5 + push.y * 0.3,
                    current.z * 0.3 + push.z
            );
            entity.velocityModified = true;
            entity.fallDistance = 0;
        }
    }

    @Override
    public void stopUsing(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {}

    @Override
    public void finishUsing(ItemStack stack, World world, LivingEntity user) {}
}