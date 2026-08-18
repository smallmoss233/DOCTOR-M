package doctor_m.Item.data_itme.KeytoTimeFragment;

import dev.emi.trinkets.api.TrinketItem;
import doctor_m.Item.KeytoTime;
import doctor_m.compat.TimelordRegenCompat;
import doctor_m.handler.KeytoTime.PocketWatchFunction;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PocketWatchItem extends TrinketItem implements KeytoTime {

    private static final int COOLDOWN_TICKS = 100;
    private static final String OWNER_KEY = "MarkedOwner";
    private static final String OWNER_NAME_KEY = "MarkedOwnerName";
    private static final String CHARGES_KEY = "Charges";
    private static final String OPEN_KEY = "Open";

    private static final String COOLDOWN_DURATION_KEY = "PocketWatchCooldownDuration";

    public PocketWatchItem(Settings settings) {
        super(settings);
    }

    public static boolean isOpen(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(OPEN_KEY) && nbt.getBoolean(OPEN_KEY);
    }

    private static void setOpen(ItemStack stack, boolean open) {
        stack.getOrCreateNbt().putBoolean(OPEN_KEY, open);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return isInCooldown(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        long remaining = getCooldownRemaining(stack);
        long total = getCooldownDuration(stack);
        if (total <= 0) return 0;
        return Math.round((float) (total - remaining) * 13.0F / (float) total);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xFFD700;
    }

    public static void startCooldown(ItemStack stack, long durationMillis) {
        long end = System.currentTimeMillis() + durationMillis;
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(PocketWatchFunction.COOLDOWN_KEY, end);
        nbt.putLong(COOLDOWN_DURATION_KEY, durationMillis);
    }

    private static boolean isInCooldown(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(PocketWatchFunction.COOLDOWN_KEY)) return false;
        long end = nbt.getLong(PocketWatchFunction.COOLDOWN_KEY);
        return System.currentTimeMillis() < end;
    }

    private static long getCooldownRemaining(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(PocketWatchFunction.COOLDOWN_KEY)) return 0;
        long end = nbt.getLong(PocketWatchFunction.COOLDOWN_KEY);
        return Math.max(0, end - System.currentTimeMillis());
    }

    private static long getCooldownDuration(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(COOLDOWN_DURATION_KEY)) {
            return getCooldownRemaining(stack);
        }
        return nbt.getLong(COOLDOWN_DURATION_KEY);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!TimelordRegenCompat.isLoaded()) {
            return super.use(world, user, hand);
        }

        ItemStack stack = user.getStackInHand(hand);

        if (isOpen(stack)) {
            setOpen(stack, false);
            return TypedActionResult.success(stack);
        }

        if (user.isSneaking()) {
            return transferRegenerations(world, user, stack);
        }

        return openPocketWatch(world, user, stack);
    }

    /**
     * 打开怀表逻辑（正常右键）
     * 主人打开时，怀表内的重生次数会自动回到玩家身上
     */
    private TypedActionResult<ItemStack> openPocketWatch(World world, PlayerEntity user, ItemStack stack) {
        setOpen(stack, true);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        UUID ownerId = getOwner(stack);
        if (ownerId == null) {
            markOwner(stack, user);
            ownerId = user.getUuid();
        }

        if (!ownerId.equals(user.getUuid())) {
            return TypedActionResult.success(stack);
        }

        int charges = getCharges(stack);
        if (charges > 0) {
            TimelordRegenCompat.RegenInfo info = TimelordRegenCompat.getRegenInfo(user);
            if (info != null) {
                int usesLeft = info.getUsesLeft();
                int max = TimelordRegenCompat.getMaxRegenerations();
                int transferable = Math.min(charges, max - usesLeft);
                if (transferable > 0) {
                    info.setUsesLeft(usesLeft + transferable);
                    setCharges(stack, charges - transferable);
                    world.playSound(null, user.getX(), user.getY(), user.getZ(),
                            SoundEvents.ITEM_TOTEM_USE, user.getSoundCategory(), 1.0F, 1.0F);
                }
            }
        }

        return TypedActionResult.success(stack);
    }

    /**
     * 重生次数储存逻辑（潜行右键，TL专属）
     * 单向存入：玩家身上的重生次数 → 怀表
     */
    private TypedActionResult<ItemStack> transferRegenerations(World world, PlayerEntity user, ItemStack stack) {
        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!TimelordRegenCompat.isTimelord(user)) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        UUID owner = getOwner(stack);
        if (owner != null && !owner.equals(user.getUuid())) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        if (owner == null) {
            markOwner(stack, user);
        }

        TimelordRegenCompat.RegenInfo info = TimelordRegenCompat.getRegenInfo(user);
        if (info == null) {
            return TypedActionResult.fail(stack);
        }

        int charges = getCharges(stack);
        int usesLeft = info.getUsesLeft();
        int max = TimelordRegenCompat.getMaxRegenerations();

        int transferable = Math.min(usesLeft, max - charges);
        if (transferable > 0) {
            info.setUsesLeft(usesLeft - transferable);
            setCharges(stack, charges + transferable);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ITEM_TOTEM_USE, user.getSoundCategory(), 1.0F, 1.0F);
        } else {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), user.getSoundCategory(), 0.5F, 1.0F);
            return TypedActionResult.success(stack, false);
        }

        // 仅在成功存入后设置冷却
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();

        if (isOpen(stack)) {
            tooltip.add(Text.translatable("message.doctor_m.pocket_watch.state.open")
                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
        } else {
            tooltip.add(Text.translatable("message.doctor_m.pocket_watch.state.closed")
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        }

        if (FabricLoader.getInstance().isModLoaded("timelordregen")) {
            int charges = (nbt != null && nbt.contains(CHARGES_KEY)) ? nbt.getInt(CHARGES_KEY) : 0;
            int max = TimelordRegenCompat.getMaxRegenerations();

            tooltip.add(Text.translatable("item.timelordregen.pocket_watch.charges", charges, max)
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));

            if (nbt != null && nbt.contains(OWNER_KEY)) {
                String ownerName = nbt.contains(OWNER_NAME_KEY) ? nbt.getString(OWNER_NAME_KEY) : null;
                if (ownerName == null && world != null) {
                    UUID ownerId = nbt.getUuid(OWNER_KEY);
                    PlayerEntity owner = world.getPlayerByUuid(ownerId);
                    if (owner != null) {
                        ownerName = owner.getName().getString();
                    }
                }
                if (ownerName != null) {
                    tooltip.add(Text.translatable("item.timelordregen.pocket_watch.owner", ownerName)
                            .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(true)));
                }
            }
        }

        if (nbt != null && nbt.contains(PocketWatchFunction.COOLDOWN_KEY)) {
            long cooldownEnd = nbt.getLong(PocketWatchFunction.COOLDOWN_KEY);
            long now = System.currentTimeMillis();
            if (now < cooldownEnd) {
                long remaining = cooldownEnd - now;
                kotlin.Pair<Integer, Integer> parts = PocketWatchFunction.getRemainingTimeParts(remaining);
                int minutes = parts.getFirst();
                int seconds = parts.getSecond();
                Text longDescription = Text.translatable("message.doctor_m.pocket_watch.cooldown", minutes, seconds);
                TooltipHelper.addWrappedTooltip(tooltip, longDescription);
            }
        }

        Text longDescription = Text.translatable("message.doctor_m.pocket_watch.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.pocket_watch.detail")
        );
    }

    private static void markOwner(ItemStack stack, PlayerEntity p) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putUuid(OWNER_KEY, p.getUuid());
        nbt.putString(OWNER_NAME_KEY, p.getName().getString());
    }

    @Nullable
    private static UUID getOwner(ItemStack stack) {
        var nbt = stack.getNbt();
        return (nbt != null && nbt.contains(OWNER_KEY)) ? nbt.getUuid(OWNER_KEY) : null;
    }

    private static int getCharges(ItemStack stack) {
        var nbt = stack.getNbt();
        return (nbt != null && nbt.contains(CHARGES_KEY)) ? nbt.getInt(CHARGES_KEY) : 0;
    }

    private static void setCharges(ItemStack stack, int v) {
        stack.getOrCreateNbt().putInt(CHARGES_KEY, v);
    }
}