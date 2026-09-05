package doctor_m.module.space_plus.Tank;

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

    // 前摇时间保持固定（也可配置，但这里不要求）
    private static final int WINDUP_TICKS = 10;
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

        // 从配置读取飞行参数
        var config = ConfigManager.getConfig();
        double thrustStrength = config.jetOxygenTankThrustStrength;
        double inertia = config.jetOxygenTankInertia;
        double gravityCompensation = config.jetOxygenTankGravityCompensation;
        double maxSpeed = config.jetOxygenTankMaxSpeed;
        double maxVerticalSpeed = config.jetOxygenTankMaxVerticalSpeed;

        // 前摇阶段
        if (ticks < WINDUP_TICKS) {
            Vec3d current = player.getVelocity();
            player.setVelocity(current.multiply(0.82));
            player.velocityModified = true;
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

        // 正式飞行
        Vec3d look = player.getRotationVector();
        Vec3d targetVel = look.multiply(thrustStrength);
        Vec3d current = player.getVelocity();

        player.setVelocity(
                current.x * inertia + targetVel.x * (1.0 - inertia),
                current.y * inertia + targetVel.y * (1.0 - inertia) + gravityCompensation,
                current.z * inertia + targetVel.z * (1.0 - inertia)
        );
        player.velocityModified = true;
        player.fallDistance = 0;

        // 限制速度
        Vec3d newVel = player.getVelocity();
        double horizontalSpeed = Math.sqrt(newVel.x * newVel.x + newVel.z * newVel.z);
        if (horizontalSpeed > maxSpeed) {
            double scale = maxSpeed / horizontalSpeed;
            player.setVelocity(newVel.x * scale, newVel.y, newVel.z * scale);
        }
        if (newVel.y > maxVerticalSpeed) {
            player.setVelocity(newVel.x, maxVerticalSpeed, newVel.z);
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
        return UseAction.NONE;
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