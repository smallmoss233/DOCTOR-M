package doctor_m.item.data_itme.fragment;

import dev.emi.trinkets.api.TrinketItem;
import doctor_m.compat.TimelordRegenCompat;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import doctor_m.world_data.TimeKey.PocketWatchFunction;
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

public class pocket_watch extends TrinketItem {

    private static final int COOLDOWN_TICKS = 100;
    private static final String OWNER_KEY = "MarkedOwner";
    private static final String CHARGES_KEY = "Charges";

    public pocket_watch(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // 没装 timelordregen → 走默认行为（就当普通物品右键）
        if (!TimelordRegenCompat.isLoaded()) {
            return super.use(world, user, hand);
        }

        ItemStack stack = user.getStackInHand(hand);
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        // 不是时间领主
        if (!TimelordRegenCompat.isTimelord(user)) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        // 所有权检查
        UUID owner = getOwner(stack);
        if (owner != null && !owner.equals(user.getUuid())) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WITHER_SPAWN, user.getSoundCategory(), 1.0F, 1.0F);
            return TypedActionResult.fail(stack);
        }

        // 首次使用标记主人
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

        int transferable;
        if (charges > usesLeft) {
            // 怀表次数多 → 传给玩家
            transferable = Math.min(charges - usesLeft, max - usesLeft);
            charges -= transferable;
            usesLeft += transferable;
        } else if (usesLeft > charges) {
            // 玩家次数多 → 传给怀表
            transferable = Math.min(usesLeft - charges, max - charges);
            usesLeft -= transferable;
            charges += transferable;
        } else {
            // 两边相等，无需传输
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), user.getSoundCategory(), 0.5F, 1.0F);
            return TypedActionResult.success(stack, false);
        }

        // 应用变更
        info.setUsesLeft(usesLeft);
        setCharges(stack, charges);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_TOTEM_USE, user.getSoundCategory(), 1.0F, 1.0F);

        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();

        // ========== 联动提示：只有装了 timelordregen 才显示 ==========
        if (FabricLoader.getInstance().isModLoaded("timelordregen")) {
            int charges = (nbt != null && nbt.contains(CHARGES_KEY)) ? nbt.getInt(CHARGES_KEY) : 0;
            int max = TimelordRegenCompat.getMaxRegenerations();

            tooltip.add(Text.translatable("item.timelordregen.pocket_watch.charges", charges, max)
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));

            if (nbt != null && nbt.contains(OWNER_KEY) && world != null) {
                UUID ownerId = nbt.getUuid(OWNER_KEY);
                PlayerEntity owner = world.getPlayerByUuid(ownerId);
                if (owner != null) {
                    tooltip.add(Text.translatable("item.timelordregen.pocket_watch.owner", owner.getName())
                            .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(true)));
                }
            }

            tooltip.add(Text.translatable("item.timelordregen.pocket_watch.desc")
                    .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(true)));
        }
        // ===========================================================

        // 原有的冷却时间显示
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

        // 原有的说明文字
        Text longDescription = Text.translatable("message.doctor_m.pocket_watch.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.pocket_watch.detail")
        );
    }

    // ---------- NBT 辅助 ----------
    private static void markOwner(ItemStack stack, PlayerEntity p) {
        stack.getOrCreateNbt().putUuid(OWNER_KEY, p.getUuid());
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