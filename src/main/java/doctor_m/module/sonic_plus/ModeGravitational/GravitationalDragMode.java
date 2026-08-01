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

    // 提取魔法数字，方便调参
    private static final double TELEPORT_THRESHOLD_SQ = 25.0; // 5^2
    private static final double DRAG_LERP = 0.4;
    private static final double ANTI_GRAVITY = 0.08;
    private static final double MAX_REACH = 10.0;
    private static final double DIST_MIN = 2.0;
    private static final double DIST_MAX = 5.0;

    private GravitationalDragMode() {
        super(0);
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.gravitational_drag")
                .formatted(Formatting.BLUE, Formatting.BOLD);
    }

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
        // 修复：user 已经是 PlayerEntity，无需重复 instanceof
        Entity target = findTargetEntity(user, MAX_REACH);
        if (target == null) return false;

        if (!world.isClient()) {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putString(DRAG_TARGET_KEY, target.getUuidAsString());
            nbt.putDouble(DRAG_DISTANCE_KEY, MathHelper.clamp(
                    user.distanceTo(target), DIST_MIN, DIST_MAX));
        }
        return true;
    }

    @Override
    public void tick(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {
        if (world.isClient()) return;
        if (!(user instanceof PlayerEntity player)) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        // 性能优化：单次 NBT 读取，避免每 tick 3 次 getNbt()
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(DRAG_TARGET_KEY, NbtCompound.STRING_TYPE)) {
            return;
        }

        Entity target = resolveTarget(nbt, serverWorld);
        if (target == null || !target.isAlive()) {
            clearDragData(stack);
            return;
        }

        double distance = nbt.contains(DRAG_DISTANCE_KEY, NbtCompound.DOUBLE_TYPE)
                ? MathHelper.clamp(nbt.getDouble(DRAG_DISTANCE_KEY), DIST_MIN, DIST_MAX)
                : 3.0;

        Vec3d eyePos = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d targetPos = eyePos.add(lookVec.multiply(distance))
                .subtract(0, target.getHeight() * 0.5, 0);

        Vec3d diff = targetPos.subtract(target.getPos());
        double distSq = diff.lengthSquared();

        if (distSq > TELEPORT_THRESHOLD_SQ) {
            // 距离过远直接瞬移（保留朝向）
            target.teleport(targetPos.x, targetPos.y, targetPos.z);
        } else {
            Vec3d velocity = diff.multiply(DRAG_LERP).add(0, ANTI_GRAVITY, 0);
            target.setVelocity(velocity);
            target.velocityDirty = true; // 1.20.1 Yarn 正确字段名
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

    @Nullable
    private static Entity resolveTarget(NbtCompound nbt, ServerWorld world) {
        UUID uuid;
        try {
            uuid = UUID.fromString(nbt.getString(DRAG_TARGET_KEY));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return world.getEntity(uuid);
    }

    private static void clearDragData(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return;
        nbt.remove(DRAG_TARGET_KEY);
        nbt.remove(DRAG_DISTANCE_KEY);
        // 防御性：若 NBT 已空，清理空 tag 减少物品序列化体积和客户端同步开销
        if (nbt.isEmpty()) {
            stack.setNbt(null);
        }
    }

    @Nullable
    private static Entity findTargetEntity(PlayerEntity player, double reach) {
        Vec3d eyePos = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d end = eyePos.add(lookVec.multiply(reach));

        Box searchBox = player.getBoundingBox()
                .stretch(lookVec.multiply(reach))
                .expand(1.0);

        EntityHitResult result = ProjectileUtil.raycast(
                player, eyePos, end, searchBox,
                entity -> !entity.isSpectator() && entity.canHit(),
                reach * reach
        );

        return result != null ? result.getEntity() : null;
    }
}