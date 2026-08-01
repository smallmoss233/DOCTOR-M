package doctor_m.module.sonic_plus.ModeChargedZeiton;

import java.util.List;

import dev.amble.ait.api.ArtronHolderItem;
import dev.amble.ait.core.item.sonic.SonicMode;
import dev.amble.ait.data.schema.sonic.SonicSchema;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PushMode extends SonicMode {
    public static final PushMode INSTANCE = new PushMode();

    private static final String COOLDOWN_KEY = "doctor_m.push_cooldown";
    private static final int COOLDOWN_TICKS = 100;
    private static final double RANGE = 5.0;
    private static final double RANGE_Y = 2.0;
    private static final double PUSH_STRENGTH = 2.8;
    private static final double PUSH_UPWARD = 0.6;

    private PushMode() {
        super(2);
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.push")
                .formatted(Formatting.YELLOW, Formatting.BOLD);
    }

    @Override
    public int maxTime() {
        return 1;
    }

    @Override
    public Identifier model(SonicSchema.Models models) {
        return SonicMode.Modes.SCANNING.model(models);
    }

    @Override
    public int fuelCost() {
        return 0;
    }

    @Override
    public boolean startUsing(ItemStack stack, World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof PlayerEntity player)) return false;

        long now = world.getTime();
        NbtCompound nbt = stack.getOrCreateNbt();

        // 冷却检查（两端都拦，防止动画不同步）
        if (nbt.contains(COOLDOWN_KEY)) {
            long end = nbt.getLong(COOLDOWN_KEY);
            if (now < end) {
                if (!world.isClient()) {
                    long sec = (end - now + 19) / 20;
                    player.sendMessage(
                            Text.translatable("message.doctor_m.sonic.cooldown", sec)
                                    .formatted(Formatting.RED), true);
                }
                return false;
            }
        }

        if (world.isClient()) return true;

        // 手动扣费
        if (stack.getItem() instanceof ArtronHolderItem holder) {
            if (holder.getCurrentFuel(stack) < 25) {
                player.sendMessage(
                        Text.translatable("message.doctor_m.sonic.insufficient_energy")
                                .formatted(Formatting.RED), true);
                return false;
            }
            holder.removeFuel(25, stack);
        }

        // 推开周围实体
        Vec3d center = player.getPos();
        Box box = Box.of(center, RANGE * 2, RANGE_Y * 2, RANGE * 2);

        List<Entity> entities = world.getOtherEntities(player, box,
                e -> e.isAlive() && !e.isSpectator());

        Vec3d playerPos = player.getPos();

        for (Entity entity : entities) {
            Vec3d dir = entity.getPos().subtract(playerPos);
            double lenSq = dir.lengthSquared();
            if (lenSq < 0.001) {
                dir = new Vec3d(0, 1, 0);
            } else {
                dir = dir.normalize();
            }

            Vec3d push = dir.multiply(PUSH_STRENGTH).add(0, PUSH_UPWARD, 0);
            entity.setVelocity(push);
            entity.velocityDirty = true; // 1.20.1 Yarn 正确字段名
            entity.fallDistance = 0;
        }

        // 记录冷却结束时间（与 PulseMode 统一，避免时间跳跃导致异常）
        nbt.putLong(COOLDOWN_KEY, now + COOLDOWN_TICKS);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.5f);

        return true;
    }

    @Override
    public void tick(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {}

    @Override
    public void stopUsing(ItemStack stack, World world, LivingEntity user, int ticks, int ticksLeft) {}

    @Override
    public void finishUsing(ItemStack stack, World world, LivingEntity user) {}
}