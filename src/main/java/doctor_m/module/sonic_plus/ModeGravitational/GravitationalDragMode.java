package doctor_m.module.sonic_plus.ModeGravitational;

import java.util.UUID;

import dev.amble.ait.core.item.sonic.SonicMode;
import dev.amble.ait.data.schema.sonic.SonicSchema;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GravitationalDragMode extends SonicMode {
    public static final GravitationalDragMode INSTANCE = new GravitationalDragMode();

    private static final String DRAG_TARGET_KEY = "doctor_m.drag_target_uuid";
    private static final String DRAG_DISTANCE_KEY = "doctor_m.drag_distance";

    private GravitationalDragMode() {
        super(0);
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.gravitational_drag")
                .formatted(Formatting.BLUE, Formatting.BOLD);
    }

    //最大牵引时间
    @Override
    public int maxTime() {
        return 72000;
    }

    @Override
    public Identifier model(SonicSchema.Models models) {
        return SonicMode.Modes.INTERACTION.model(models);
    }

    @Override
    public int fuelCost() {
        return 1;
    }

    @Override
    public boolean startUsing(ItemStack stack, World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof PlayerEntity player)) return false;

        Entity target = findTargetEntity(player, 10);
        if (target == null) return false;

        if (!world.isClient()) {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putString(DRAG_TARGET_KEY, target.getUuidAsString());
            nbt.putDouble(DRAG_DISTANCE_KEY, MathHelper.clamp(player.distanceTo(target), 2.0, 5.0));
        }

        return true;
    }

    @Override
    public void tick(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {
        if (world.isClient()) return;
        if (!(user instanceof PlayerEntity player)) return;

        Entity target = getDragTarget(stack, world);
        if (target == null || !target.isAlive()) {
            clearDragData(stack);
            return;
        }

        Vec3d eyePos = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        double distance = getDragDistance(stack);

        Vec3d targetPos = eyePos.add(lookVec.multiply(distance));
        targetPos = targetPos.subtract(0, target.getHeight() * 0.5, 0);

        Vec3d currentPos = target.getPos();
        Vec3d diff = targetPos.subtract(currentPos);

        if (diff.lengthSquared() > 25.0) {
            target.teleport(targetPos.x, targetPos.y, targetPos.z);
        } else {
            Vec3d velocity = diff.multiply(0.4).add(0, 0.08, 0);
            target.setVelocity(velocity);
            target.velocityModified = true;
        }

        target.fallDistance = 0;
    }

    @Override
    public void stopUsing(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {
        if (!world.isClient()) clearDragData(stack);
    }

    @Override
    public void finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient()) clearDragData(stack);
    }

    /* ==========================================
       内部工具方法
       ========================================== */
    private static @Nullable Entity getDragTarget(ItemStack stack, World world) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(DRAG_TARGET_KEY)) return null;

        UUID uuid;
        try {
            uuid = UUID.fromString(nbt.getString(DRAG_TARGET_KEY));
        } catch (IllegalArgumentException e) {
            return null;
        }

        if (!(world instanceof ServerWorld serverWorld)) return null;
        return serverWorld.getEntity(uuid);
    }

    private static double getDragDistance(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(DRAG_DISTANCE_KEY)) return 3.0;
        return MathHelper.clamp(nbt.getDouble(DRAG_DISTANCE_KEY), 2.0, 5.0);
    }

    private static void clearDragData(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            nbt.remove(DRAG_TARGET_KEY);
            nbt.remove(DRAG_DISTANCE_KEY);
        }
    }

    private static @Nullable Entity findTargetEntity(PlayerEntity player, double reach) {
        Vec3d eyePos = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d end = eyePos.add(lookVec.multiply(reach));

        Box searchBox = player.getBoundingBox()
                .stretch(lookVec.multiply(reach))
                .expand(1.0, 1.0, 1.0);

        EntityHitResult result = ProjectileUtil.raycast(
                player, eyePos, end, searchBox,
                entity -> !entity.isSpectator() && entity.canHit(),
                reach * reach
        );

        return result != null ? result.getEntity() : null;
    }
}