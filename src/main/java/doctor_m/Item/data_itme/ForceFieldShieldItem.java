package doctor_m.Item.data_itme;

import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;

import java.util.List;

public class ForceFieldShieldItem extends Item {

    public static final double SHIELD_RADIUS = 3.5;

    private static final String ENERGY_KEY = "force_field_energy";
    private static final String COOLING_KEY = "force_field_cooling";
    private static final String COOLDOWN_KEY = "force_field_cooldown";

    private static final ModConfig CONFIG = ConfigManager.getConfig();

    public ForceFieldShieldItem(Settings settings) {
        super(settings.maxCount(1));
    }

    /* ========== 使用逻辑 ========== */

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !(user instanceof PlayerEntity player)) return;

        long time = world.getTime();
        if (time % 20 == 0) {
            boolean active = !isCooling(stack) && getCooldown(stack) <= 0 && getEnergy(stack) > 0;
            player.sendMessage(
                    Text.translatable(active
                                    ? "message.doctor_m.force_field_shield.active"
                                    : "message.doctor_m.force_field_shield.blocking")
                            .formatted(active ? Formatting.RED : Formatting.GRAY),
                    true
            );
        }

        if (isCooling(stack) || getCooldown(stack) > 0) return;

        int energy = getEnergy(stack);
        int drain = CONFIG.forceFieldDrainPerTick;
        if (energy >= drain) {
            int newEnergy = energy - drain;
            setEnergy(stack, newEnergy);
            applyForceFieldEffects(world, player);
            if (newEnergy < drain) {
                setCooling(stack, true);
            }
        } else {
            setCooling(stack, true);
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
        if (world.isClient || !(user instanceof PlayerEntity player)) return;

        if (isCooling(stack) || getCooldown(stack) > 0 || getEnergy(stack) <= 0) return;

        applyReleasePush(world, player);
        setCooldown(stack, CONFIG.forceFieldCooldownTicks);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient || !(entity instanceof PlayerEntity player)) return;
        if (world.getTime() % 4 != 0) return;

        NbtCompound nbt = stack.getOrCreateNbt();

        int cd = nbt.getInt(COOLDOWN_KEY);
        if (cd > 0) {
            nbt.putInt(COOLDOWN_KEY, cd - 4);
        }

        boolean isUsingThis = player.isUsingItem() && player.getActiveItem().getItem() == this;
        boolean cooling = nbt.getBoolean(COOLING_KEY);

        if (!cooling && cd <= 0 && isUsingThis) return;

        int current = nbt.contains(ENERGY_KEY) ? nbt.getInt(ENERGY_KEY) : CONFIG.forceFieldMaxEnergy;
        int max = CONFIG.forceFieldMaxEnergy;
        if (current < max) {
            int newEnergy = Math.min(max, current + CONFIG.forceFieldRechargePerTick * 4);
            if (newEnergy != current) {
                nbt.putInt(ENERGY_KEY, newEnergy);
                if (newEnergy >= max && cooling) {
                    nbt.putBoolean(COOLING_KEY, false);
                }
            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    /* ========== 提示 ========== */

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("message.doctor_m.force_field_shield.energy",
                getEnergy(stack), CONFIG.forceFieldMaxEnergy).formatted(Formatting.GRAY));
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.force_field_shield.detail"));
    }

    /* ========== 能量条 ========== */

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round((float) getEnergy(stack) * 13.0F / (float) CONFIG.forceFieldMaxEnergy);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        if (isCooling(stack)) return 0x808080;
        if (getCooldown(stack) > 0) return 0xFF8C00;
        return 0xFF0000;
    }

    /* ========== NBT ========== */

    public static int getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return (nbt != null && nbt.contains(ENERGY_KEY)) ? nbt.getInt(ENERGY_KEY) : CONFIG.forceFieldMaxEnergy;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        int clamped = Math.min(CONFIG.forceFieldMaxEnergy, Math.max(0, energy));
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getInt(ENERGY_KEY) != clamped) {
            nbt.putInt(ENERGY_KEY, clamped);
        }
    }

    public static boolean isCooling(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(COOLING_KEY);
    }

    public static void setCooling(ItemStack stack, boolean cooling) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getBoolean(COOLING_KEY) != cooling) {
            nbt.putBoolean(COOLING_KEY, cooling);
        }
    }

    public static int getCooldown(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getInt(COOLDOWN_KEY) : 0;
    }

    public static void setCooldown(ItemStack stack, int ticks) {
        int clamped = Math.max(0, ticks);
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getInt(COOLDOWN_KEY) != clamped) {
            nbt.putInt(COOLDOWN_KEY, clamped);
        }
    }

    public static boolean isForceFieldActive(PlayerEntity player) {
        if (!player.isUsingItem()) return false;
        ItemStack stack = player.getActiveItem();
        if (!(stack.getItem() instanceof ForceFieldShieldItem)) return false;
        return !isCooling(stack) && getCooldown(stack) <= 0 && getEnergy(stack) > 0;
    }

    /* ========== 伤害类型判断（供 Mixin 调用） ========== */

    public static boolean isEnvironmentalOrSpecialDamage(DamageSource source) {
        return source.isOf(DamageTypes.IN_FIRE)
                || source.isOf(DamageTypes.ON_FIRE)
                || source.isOf(DamageTypes.LAVA)
                || source.isOf(DamageTypes.HOT_FLOOR)
                || source.isOf(DamageTypes.IN_WALL)
                || source.isOf(DamageTypes.CRAMMING)
                || source.isOf(DamageTypes.DROWN)
                || source.isOf(DamageTypes.STARVE)
                || source.isOf(DamageTypes.CACTUS)
                || source.isOf(DamageTypes.FALL)
                || source.isOf(DamageTypes.FLY_INTO_WALL)
                || source.isOf(DamageTypes.OUT_OF_WORLD)
                || source.isOf(DamageTypes.GENERIC)
                || source.isOf(DamageTypes.MAGIC)
                || source.isOf(DamageTypes.WITHER)
                || source.isOf(DamageTypes.DRAGON_BREATH)
                || source.isOf(DamageTypes.SWEET_BERRY_BUSH)
                || source.isOf(DamageTypes.FREEZE)
                || source.isOf(DamageTypes.STALAGMITE)
                || source.isOf(DamageTypes.OUTSIDE_BORDER)
                || source.isOf(DamageTypes.GENERIC_KILL)
                || source.isOf(DamageTypes.LIGHTNING_BOLT);
    }

    /* ========== 力场效果（圆形范围 + 推开一切非掉落物） ========== */

    private void applyForceFieldEffects(World world, PlayerEntity player) {
        Vec3d centerPos = player.getPos().add(0, player.getHeight() / 2.0, 0);
        double radius = SHIELD_RADIUS;
        // Box 粗筛，再用距离精筛实现真圆形
        Box box = Box.of(centerPos, radius * 2, radius * 2, radius * 2);
        double radiusSq = radius * radius;

        boolean playedSoundThisTick = false;
        for (Entity entity : world.getOtherEntities(player, box)) {
            // 排除掉落物，其余全部处理
            if (entity instanceof ItemEntity) continue;

            Vec3d diff = entity.getPos().subtract(centerPos);
            double distSq = diff.lengthSquared();

            // 圆形范围过滤
            if (distSq > radiusSq) continue;
            if (distSq < 1.0E-4) continue;

            Vec3d pushDir = diff.normalize();

            // 弹射物直接销毁
            if (entity instanceof ProjectileEntity projectile) {
                if (!playedSoundThisTick) {
                    world.playSound(null, projectile.getBlockPos(), SoundEvents.ENTITY_GENERIC_BURN,
                            SoundCategory.PLAYERS, 0.8f, 1.2f);
                    playedSoundThisTick = true;
                }
                projectile.discard();
                continue;
            }

            // 推开一切其他实体（包括玩家、不可推动的实体等）
            Vec3d motion = pushDir.multiply(CONFIG.forceFieldPushStrength);
            entity.setVelocity(entity.getVelocity().add(motion));
            entity.velocityDirty = true;
        }
    }

    /* ========== 释放推力（也改成圆形） ========== */

    private void applyReleasePush(World world, PlayerEntity player) {
        Vec3d centerPos = player.getPos().add(0, player.getHeight() / 2.0, 0);
        double radius = CONFIG.forceFieldReleaseRadius;
        Box box = Box.of(centerPos, radius * 2, radius * 2, radius * 2);
        double radiusSq = radius * radius;

        for (Entity entity : world.getOtherEntities(player, box)) {
            if (entity instanceof ItemEntity) continue;
            if (entity instanceof PlayerEntity) continue;

            Vec3d diff = entity.getPos().subtract(centerPos);
            double distSq = diff.lengthSquared();
            if (distSq > radiusSq) continue;
            if (distSq < 1.0E-4) continue;

            Vec3d dir = diff.normalize();
            Vec3d motion = new Vec3d(
                    dir.x * CONFIG.forceFieldReleaseStrength,
                    CONFIG.forceFieldReleaseUpward,
                    dir.z * CONFIG.forceFieldReleaseStrength
            );
            entity.setVelocity(entity.getVelocity().add(motion));
            entity.velocityDirty = true;
        }

        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK,
                SoundCategory.PLAYERS, 1.0f, 0.8f);
    }
}