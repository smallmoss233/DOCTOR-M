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
    private static final int COOLDOWN_TICKS = 100; // 5秒
    private static final double RANGE = 5.0;
    private static final double PUSH_STRENGTH = 2.8;

    private PushMode() {
        super(2);
    }

    @Override
    public Text text() {
        return Text.translatable("sonic_mode.doctor_m.push")
                .formatted(Formatting.YELLOW, Formatting.BOLD); // 金色
    }

    @Override
    public int maxTime() {
        return 1; // 瞬间释放
    }

    @Override
    public Identifier model(SonicSchema.Models models) {
        return SonicMode.Modes.SCANNING.model(models);
    }

    @Override
    public int fuelCost() {
        return 0; // 手动扣除，避免 usageTick 扣费逻辑干扰
    }

    @Override
    public boolean startUsing(ItemStack stack, World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof PlayerEntity player)) return false;

        NbtCompound nbt = stack.getOrCreateNbt();
        long currentTick = world.getTime();

        // 检查冷却
        if (nbt.contains(COOLDOWN_KEY)) {
            long lastUse = nbt.getLong(COOLDOWN_KEY);
            long remaining = (lastUse + COOLDOWN_TICKS) - currentTick;
            if (remaining > 0) {
                if (!world.isClient()) {
                    player.sendMessage(Text.literal("冷却中: " + (remaining / 20 + 1) + "s")
                            .formatted(Formatting.RED), true);
                }
                return false;
            }
        }

        if (world.isClient()) return true;

        // 手动扣除 25 AU
        if (stack.getItem() instanceof ArtronHolderItem holder) {
            if (holder.getCurrentFuel(stack) < 25) {
                player.sendMessage(Text.literal("能量不足！").formatted(Formatting.RED), true);
                return false;
            }
            holder.removeFuel(25, stack);
        }

        // 猛地推开周围实体
        Vec3d center = player.getPos();
        Box box = new Box(
                center.x - RANGE, center.y - 2, center.z - RANGE,
                center.x + RANGE, center.y + 2, center.z + RANGE
        );

        List<Entity> entities = world.getOtherEntities(player, box,
                e -> e.isAlive() && !e.isSpectator());

        for (Entity entity : entities) {
            Vec3d dir = entity.getPos().subtract(player.getPos());
            if (dir.lengthSquared() < 0.001) {
                dir = new Vec3d(0, 1, 0);
            } else {
                dir = dir.normalize();
            }

            Vec3d push = dir.multiply(PUSH_STRENGTH).add(0, 0.6, 0);
            entity.setVelocity(push);
            entity.velocityModified = true;
            entity.fallDistance = 0;
        }

        // 记录冷却
        nbt.putLong(COOLDOWN_KEY, currentTick);

        // 音效
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