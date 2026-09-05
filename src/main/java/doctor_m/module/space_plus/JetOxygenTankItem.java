package doctor_m.module.space_plus;

import doctor_m.config.ConfigManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class JetOxygenTankItem extends Item {

    public static final String OXYGEN_KEY = "oxygen";

    // 飞行参数（可配置）
    private static final int WINDUP_TICKS = 10;               // 前摇时间
    private static final double THRUST_STRENGTH = 0.15;       // 目标速度
    private static final double INERTIA = 0.78;               // 惯性保留比例
    private static final double GRAVITY_COMPENSATION = 0.08;  // 重力补偿
    private static final double MAX_SPEED = 2.5;
    private static final double MAX_VERTICAL_SPEED = 2.0;
    private static final double OXYGEN_CONSUMPTION_PER_TICK = 1.0;

    public JetOxygenTankItem(Settings settings) {
        super(settings);
    }

    /** 最大氧气量，直接使用普通氧气瓶的配置 */
    public double getMaxOxygen() {
        return ConfigManager.getConfig().oxygenTankMaxOxygen;
    }

    public double getOxygen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OXYGEN_KEY) ? nbt.getDouble(OXYGEN_KEY) : 0.0;
    }

    public void setOxygen(ItemStack stack, double amount) {
        double max = getMaxOxygen();
        stack.getOrCreateNbt().putDouble(OXYGEN_KEY, Math.min(amount, max));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            if (getOxygen(stack) <= 0) {
                user.sendMessage(Text.translatable("message.doctor_m.oxygen_tank.empty"), true);
                return TypedActionResult.fail(stack);
            }
        }

        // 开始持续使用，触发 usageTick
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

        int ticks = this.getMaxUseTime(stack) - remainingUseTicks;

        double oxygen = getOxygen(stack);
        if (oxygen <= 0) {
            player.stopUsingItem();
            return;
        }

        // 前摇阶段：减速蓄力
        if (ticks < WINDUP_TICKS) {
            Vec3d current = player.getVelocity();
            player.setVelocity(current.multiply(0.82));
            player.velocityModified = true;   // 关键：标记速度已修改，确保同步
            player.fallDistance = 0;

            if (world instanceof ServerWorld serverWorld) {
                if (ticks % 3 == 0) {
                    Vec3d center = player.getPos().add(0, player.getHeight() * 0.5, 0);
                    serverWorld.spawnParticles(ParticleTypes.END_ROD,
                            center.x, center.y, center.z,
                            1, 0, 0.03, 0, 0.01);
                }
            }
            setOxygen(stack, oxygen - OXYGEN_CONSUMPTION_PER_TICK * 0.5);
            return;
        }

        // 正式飞行：惯性混合 + 推力 + 重力补偿
        Vec3d look = player.getRotationVector();
        Vec3d targetVel = look.multiply(THRUST_STRENGTH);
        Vec3d current = player.getVelocity();

        player.setVelocity(
                current.x * INERTIA + targetVel.x * (1.0 - INERTIA),
                current.y * INERTIA + targetVel.y * (1.0 - INERTIA) + GRAVITY_COMPENSATION,
                current.z * INERTIA + targetVel.z * (1.0 - INERTIA)
        );
        player.velocityModified = true;   // 关键
        player.fallDistance = 0;

        // 限制速度
        Vec3d newVel = player.getVelocity();
        double horizontalSpeed = Math.sqrt(newVel.x * newVel.x + newVel.z * newVel.z);
        if (horizontalSpeed > MAX_SPEED) {
            double scale = MAX_SPEED / horizontalSpeed;
            player.setVelocity(newVel.x * scale, newVel.y, newVel.z * scale);
        }
        if (newVel.y > MAX_VERTICAL_SPEED) {
            player.setVelocity(newVel.x, MAX_VERTICAL_SPEED, newVel.z);
        }

        setOxygen(stack, oxygen - OXYGEN_CONSUMPTION_PER_TICK);

        // 粒子与音效
        if (world instanceof ServerWorld serverWorld) {
            if (player.age % 2 == 0) {
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        1, 0, 0, 0, 0.01);
                serverWorld.spawnParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        1, 0, 0, 0, 0.01);
            }
            if (player.age % 10 == 0) {
                serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.3f, 1.5f);
            }
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.5f, 0.8f);
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE; // 不使用任何动作动画
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        double oxygen = getOxygen(stack);
        tooltip.add(Text.translatable("tooltip.doctor_m.oxygen", oxygen, getMaxOxygen()));
        tooltip.add(Text.translatable("message.doctor_m.jet_oxygen_tank").formatted(Formatting.GOLD));
    }
}