package doctor_m.Item.data_itme;

import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ForceFieldShieldItem extends Item {

    public static final double SHIELD_RADIUS = 3.5;

    private static final String ENERGY_KEY = "force_field_energy";
    private static final String COOLING_KEY = "force_field_cooling";
    private static final String COOLDOWN_KEY = "force_field_cooldown";

    public ForceFieldShieldItem(Settings settings) {
        super(settings.maxCount(1));
    }

    private static ModConfig cfg() {
        return ConfigManager.getConfig();
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

        // ===== 每秒提示一次（20 tick）=====
        if (world.getTime() % 20 == 0) {
            boolean fieldActive = !isCooling(stack) && getCooldown(stack) <= 0 && getEnergy(stack) > 0;

            if (fieldActive) {
                player.sendMessage(
                        Text.translatable("message.doctor_m.force_field_shield.active")
                                .formatted(Formatting.RED),
                        true
                );
            } else {
                player.sendMessage(
                        Text.translatable("message.doctor_m.force_field_shield.blocking")
                                .formatted(Formatting.GRAY),
                        true
                );
            }
        }

        // 原有逻辑：冷却/耗尽期间只格挡，不耗能不力场
        if (isCooling(stack) || getCooldown(stack) > 0) return;

        int energy = getEnergy(stack);
        int drain = cfg().forceFieldDrainPerTick;
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

        if (isCooling(stack)) return;
        if (getCooldown(stack) > 0) return;
        if (getEnergy(stack) <= 0) return;

        applyReleasePush(world, player);
        setCooldown(stack, cfg().forceFieldCooldownTicks);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient || !(entity instanceof PlayerEntity player)) return;

        // ===== 核心修复：每 4 tick 处理一次，避免手持动画乱跳 =====
        if (world.getTime() % 4 != 0) return;

        // 主动冷却倒计时（每 4 tick 减 4）
        int cd = getCooldown(stack);
        if (cd > 0) {
            setCooldown(stack, cd - 4);
        }

        boolean isUsingThis = player.isUsingItem() && player.getActiveItem().getItem() == this;

        // 力场正常运行中 → 不回能量
        if (!isCooling(stack) && getCooldown(stack) <= 0 && isUsingThis) return;

        // 回能
        int current = getEnergy(stack);
        int max = cfg().forceFieldMaxEnergy;
        if (current < max) {
            int newEnergy = Math.min(max, current + cfg().forceFieldRechargePerTick * 4);
            if (newEnergy != current) {
                setEnergy(stack, newEnergy);
                if (newEnergy >= max && isCooling(stack)) {
                    setCooling(stack, false);
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

        int energy = getEnergy(stack);
        int max = cfg().forceFieldMaxEnergy;

        tooltip.add(Text.translatable("message.doctor_m.force_field_shield.energy", energy, max)
                .formatted(Formatting.GRAY));

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
        return Math.round((float) getEnergy(stack) * 13.0F / (float) cfg().forceFieldMaxEnergy);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        if (isCooling(stack)) return 0x808080;
        if (getCooldown(stack) > 0) return 0xFF8C00;
        return 0xFF0000;
    }

    /* ========== NBT（全部加"值不同才写"保护） ========== */

    public static int getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.contains(ENERGY_KEY) ? nbt.getInt(ENERGY_KEY) : cfg().forceFieldMaxEnergy;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        int clamped = Math.min(cfg().forceFieldMaxEnergy, Math.max(0, energy));
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getInt(ENERGY_KEY) != clamped) {
            nbt.putInt(ENERGY_KEY, clamped);
        }
    }

    public static boolean isCooling(ItemStack stack) {
        return stack.getOrCreateNbt().getBoolean(COOLING_KEY);
    }

    public static void setCooling(ItemStack stack, boolean cooling) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getBoolean(COOLING_KEY) != cooling) {
            nbt.putBoolean(COOLING_KEY, cooling);
        }
    }

    public static int getCooldown(ItemStack stack) {
        return stack.getOrCreateNbt().getInt(COOLDOWN_KEY);
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

    /* ========== 力场效果 ========== */

    private void applyForceFieldEffects(World world, PlayerEntity player) {
        Vec3d centerPos = player.getPos().add(0, player.getHeight() / 2.0, 0);
        Box box = new Box(centerPos, centerPos).expand(SHIELD_RADIUS);

        world.getOtherEntities(player, box).stream()
                .filter(e -> e.isPushable() || e instanceof ProjectileEntity)
                .forEach(entity -> {
                    Vec3d pushDir = entity.getPos().subtract(centerPos).normalize();

                    if (entity instanceof ProjectileEntity projectile) {
                        BlockPos pos = projectile.getBlockPos();
                        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_BURN,
                                SoundCategory.PLAYERS, 0.8f, 1.2f);
                        projectile.discard();
                        return;
                    }

                    Vec3d motion = pushDir.multiply(cfg().forceFieldPushStrength);
                    entity.setVelocity(entity.getVelocity().add(motion));
                    entity.velocityDirty = true;
                    entity.velocityModified = true;
                });
    }

    private void applyReleasePush(World world, PlayerEntity player) {
        Vec3d centerPos = player.getPos().add(0, player.getHeight() / 2.0, 0);
        Box box = new Box(centerPos, centerPos).expand(cfg().forceFieldReleaseRadius);

        world.getOtherEntities(player, box).stream()
                .filter(e -> e.isPushable() && !(e instanceof PlayerEntity))
                .forEach(entity -> {
                    Vec3d dir = entity.getPos().subtract(centerPos).normalize();
                    Vec3d motion = new Vec3d(
                            dir.x * cfg().forceFieldReleaseStrength,
                            cfg().forceFieldReleaseUpward,
                            dir.z * cfg().forceFieldReleaseStrength
                    );
                    entity.setVelocity(entity.getVelocity().add(motion));
                    entity.velocityDirty = true;
                    entity.velocityModified = true;
                });

        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK,
                SoundCategory.PLAYERS, 1.0f, 0.8f);
    }
}