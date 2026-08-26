package doctor_m.Item.data_item;

import dev.amble.ait.core.item.ArtronCollectorItem;
import doctor_m.Item.items;
import doctor_m.util.VMClientScreenOpener;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VortexManipulatorItem extends Item {

    // ========== NBT Keys ==========
    public static final String DEST_X = "DestX";
    public static final String DEST_Y = "DestY";
    public static final String DEST_Z = "DestZ";
    public static final String DEST_DIM = "DestDim";
    public static final String PREV_X = "PrevX";
    public static final String PREV_Y = "PrevY";
    public static final String PREV_Z = "PrevZ";
    public static final String PREV_DIM = "PrevDim";
    public static final String FUEL = "Fuel";
    public static final String OVERHEAT = "Overheat";
    public static final String LAST_USED = "LastUsed";
    public static final String BROKEN_UNTIL = "BrokenUntil";
    public static final String COOLDOWN_END_SYS = "CooldownEndSys";

    // ========== Constants ==========
    public static final int MAX_FUEL = 1500;
    public static final int MAX_OVERHEAT = 100;
    public static final int COOLDOWN_TICKS = 60 * 20;
    public static final long BROKEN_COOLDOWN_TICKS = 3L * 24000L;

    public static final int COLOR_FUEL = 0xFFD700;
    public static final int COLOR_COOLDOWN = 0xFF3333;
    public static final int COLOR_BROKEN = 0xAA0000;

    public VortexManipulatorItem(Settings settings) {
        super(settings);
    }

    // ========== Client: Item Bar ==========
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        if (getBrokenUntil(stack) > 0) return 13;
        return Math.round(13f * getFuel(stack) / MAX_FUEL);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        if (getBrokenUntil(stack) > 0) return COLOR_BROKEN;
        if (isOnCooldownSys(stack)) return COLOR_COOLDOWN;
        return COLOR_FUEL;
    }

    // ========== Client: Tooltip ==========
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        String dimId = getDestDim(stack);
        if (!dimId.isEmpty()) {
            Text dimText = getDimensionDisplayText(dimId);
            tooltip.add(Text.translatable("tooltip.doctor_m.vm.destination",
                            (int) getDestX(stack), (int) getDestY(stack), (int) getDestZ(stack))
                    .append(Text.literal(" @ ")).append(dimText)
                    .formatted(Formatting.GRAY));
        }

        int fuel = getFuel(stack);
        int overheat = getOverheat(stack);
        long brokenUntil = getBrokenUntil(stack);

        tooltip.add(Text.translatable("tooltip.doctor_m.vm.fuel", fuel, MAX_FUEL)
                .formatted(fuel < 100 ? Formatting.RED : Formatting.GOLD));

        tooltip.add(Text.translatable("tooltip.doctor_m.vm.heat", overheat)
                .formatted(overheat > 80 ? Formatting.DARK_RED : Formatting.YELLOW));

        // 客户端冷却显示（基于真实时间，不受维度影响）
        if (isOnCooldownSys(stack) && brokenUntil == 0) {
            long remainingMs = getCooldownEndSys(stack) - System.currentTimeMillis();
            int sec = Math.max(0, (int) (remainingMs / 1000));
            tooltip.add(Text.translatable("tooltip.doctor_m.vm.cooldown", sec)
                    .formatted(Formatting.RED, Formatting.BOLD));
        }

        if (brokenUntil > 0 && world != null) {
            long remaining = brokenUntil - getOverworldTime(world);
            if (remaining > 0) {
                int days = (int) (remaining / 24000);
                int hours = (int) ((remaining % 24000) / 1000);
                tooltip.add(Text.translatable("tooltip.doctor_m.vm.broken", days, hours)
                        .formatted(Formatting.DARK_RED, Formatting.BOLD));
            }
        }

        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.vm.detail"));

        super.appendTooltip(stack, world, tooltip, context);
    }

    // ========== Use ==========
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // 服务端：自动修复检测（统一使用 Overworld 时间）
        if (!world.isClient()) {
            tryAutoRepair(stack, world);
        }

        // 潜行主手：尝试加载路径点 / 充电
        if (!world.isClient() && player.isSneaking() && hand == Hand.MAIN_HAND) {
            ItemStack offhand = player.getOffHandStack();
            if (tryLoadWaypoint(player, stack, offhand)) {
                return TypedActionResult.success(stack);
            }
            if (offhand.getItem() instanceof ArtronCollectorItem) {
                return chargeFromCollector(player, stack, offhand);
            }
        }

        // 目的地为空时自动保存当前位置
        if (!world.isClient() && getDestDim(stack).isEmpty()) {
            saveCurrentAsDest(player, stack);
        }

        if (world.isClient()) {
            openScreen(player, stack);
        }
        return TypedActionResult.success(stack);
    }

    // ========== Server: Inventory Tick (散热) ==========
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;
        if (getBrokenUntil(stack) != 0) return; // 已损坏不散热

        long time = getOverworldTime(world);
        if (time % 80 != 0) return; // 每秒执行一次

        int overheat = getOverheat(stack);
        if (overheat > 0) {
            setOverheat(stack, overheat - 1);
        }
    }

    // ========== Logic Helpers ==========

    private static boolean tryLoadWaypoint(PlayerEntity player, ItemStack vmStack, ItemStack offhand) {
        if (offhand.isEmpty()) return false;
        NbtCompound nbt = offhand.getNbt();
        if (nbt == null || !nbt.contains("pos", net.minecraft.nbt.NbtElement.COMPOUND_TYPE)) return false;

        NbtCompound pos = nbt.getCompound("pos");
        if (!pos.contains("X") || !pos.contains("Y") || !pos.contains("Z") || !pos.contains("dimension")) {
            return false;
        }

        setDestX(vmStack, pos.getInt("X"));
        setDestY(vmStack, pos.getInt("Y"));
        setDestZ(vmStack, pos.getInt("Z"));
        setDestDim(vmStack, pos.getString("dimension"));

        String name = offhand.hasCustomName()
                ? offhand.getName().getString()
                : offhand.getItem().getName(offhand).getString();
        player.sendMessage(Text.translatable("message.doctor_m.vm.waypoint_loaded", name)
                .formatted(Formatting.GREEN), true);
        return true;
    }

    private static TypedActionResult<ItemStack> chargeFromCollector(PlayerEntity player, ItemStack vmStack, ItemStack collectorStack) {
        int vmFuel = getFuel(vmStack);
        if (vmFuel >= MAX_FUEL) {
            player.sendMessage(Text.translatable("message.doctor_m.vm.fully_charged")
                    .formatted(Formatting.YELLOW), true);
            return TypedActionResult.fail(vmStack);
        }

        double cellFuel = ArtronCollectorItem.getFuel(collectorStack);
        if (cellFuel <= 0) {
            player.sendMessage(Text.translatable("message.doctor_m.vm.collector_empty")
                    .formatted(Formatting.RED), true);
            return TypedActionResult.fail(vmStack);
        }

        int space = MAX_FUEL - vmFuel;
        int transfer = (int) Math.min(cellFuel, space);

        setFuel(vmStack, vmFuel + transfer);
        NbtCompound nbt = collectorStack.getOrCreateNbt();
        nbt.putDouble(ArtronCollectorItem.AU_LEVEL, cellFuel - transfer);

        player.sendMessage(Text.translatable("message.doctor_m.vm.charged", transfer, getFuel(vmStack), MAX_FUEL)
                .formatted(Formatting.GREEN), true);
        return TypedActionResult.success(vmStack);
    }

    public static void punishBrokenUse(PlayerEntity player) {
        player.damage(player.getDamageSources().generic(), 6.0f);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0));
        player.sendMessage(Text.translatable("message.doctor_m.vm.broken_use_injured")
                .formatted(Formatting.DARK_RED), true);
    }

    @Environment(EnvType.CLIENT)
    private static void openScreen(PlayerEntity player, ItemStack stack) {
        if (VMClientScreenOpener.opener != null) {
            VMClientScreenOpener.opener.open(player, stack);
        }
    }

    // ========== Time Utilities ==========

    /** 获取 Overworld 时间，作为跨维度统一时间基准 */
    public static long getOverworldTime(World world) {
        if (world instanceof net.minecraft.server.world.ServerWorld sw && sw.getServer() != null) {
            return sw.getServer().getOverworld().getTime();
        }
        return world.getTime(); // fallback
    }

    /** 尝试自动修复（损坏时间到期） */
    public static void tryAutoRepair(ItemStack stack, World world) {
        long brokenUntil = getBrokenUntil(stack);
        if (brokenUntil == 0) return;
        long time = getOverworldTime(world);
        if (brokenUntil <= time) {
            setBrokenUntil(stack, 0);
            setOverheat(stack, 0);
        }
    }

    // ========== NBT Accessors（只读用 getNbt，避免空 Tag 污染） ==========

    public static double getDestX(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getDouble(DEST_X) : 0;
    }
    public static void setDestX(ItemStack stack, double v) {
        stack.getOrCreateNbt().putDouble(DEST_X, v);
    }

    public static double getDestY(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getDouble(DEST_Y) : 0;
    }
    public static void setDestY(ItemStack stack, double v) {
        stack.getOrCreateNbt().putDouble(DEST_Y, v);
    }

    public static double getDestZ(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getDouble(DEST_Z) : 0;
    }
    public static void setDestZ(ItemStack stack, double v) {
        stack.getOrCreateNbt().putDouble(DEST_Z, v);
    }

    public static String getDestDim(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(DEST_DIM) : "";
    }
    public static void setDestDim(ItemStack stack, String v) {
        stack.getOrCreateNbt().putString(DEST_DIM, v);
    }

    public static double getPrevX(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getDouble(PREV_X) : 0;
    }
    public static void setPrevX(ItemStack stack, double v) {
        stack.getOrCreateNbt().putDouble(PREV_X, v);
    }

    public static double getPrevY(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getDouble(PREV_Y) : 0;
    }
    public static void setPrevY(ItemStack stack, double v) {
        stack.getOrCreateNbt().putDouble(PREV_Y, v);
    }

    public static double getPrevZ(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getDouble(PREV_Z) : 0;
    }
    public static void setPrevZ(ItemStack stack, double v) {
        stack.getOrCreateNbt().putDouble(PREV_Z, v);
    }

    public static String getPrevDim(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(PREV_DIM) : "";
    }
    public static void setPrevDim(ItemStack stack, String v) {
        stack.getOrCreateNbt().putString(PREV_DIM, v);
    }

    public static int getFuel(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getInt(FUEL) : 0;
    }
    public static void setFuel(ItemStack stack, int v) {
        stack.getOrCreateNbt().putInt(FUEL, Math.min(v, MAX_FUEL));
    }

    public static int getOverheat(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getInt(OVERHEAT) : 0;
    }
    public static void setOverheat(ItemStack stack, int v) {
        stack.getOrCreateNbt().putInt(OVERHEAT, Math.min(v, MAX_OVERHEAT));
    }

    public static long getLastUsed(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getLong(LAST_USED) : 0;
    }
    public static void setLastUsed(ItemStack stack, long v) {
        stack.getOrCreateNbt().putLong(LAST_USED, v);
    }

    public static long getBrokenUntil(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getLong(BROKEN_UNTIL) : 0;
    }
    public static void setBrokenUntil(ItemStack stack, long v) {
        stack.getOrCreateNbt().putLong(BROKEN_UNTIL, v);
    }

    public static long getCooldownEndSys(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getLong(COOLDOWN_END_SYS) : 0;
    }
    public static void setCooldownEndSys(ItemStack stack, long v) {
        stack.getOrCreateNbt().putLong(COOLDOWN_END_SYS, v);
    }

    // ========== Derived State ==========

    /** 客户端专用：基于真实时间的冷却显示 */
    public static boolean isOnCooldownSys(ItemStack stack) {
        return getCooldownEndSys(stack) > System.currentTimeMillis();
    }

    /** 服务端权威：基于 Overworld tick 的冷却判定 */
    public static boolean isOnCooldown(ItemStack stack, long overworldTime) {
        return overworldTime - getLastUsed(stack) < COOLDOWN_TICKS;
    }

    public static int getCooldownRemaining(ItemStack stack, long overworldTime) {
        return (int) Math.max(0, (COOLDOWN_TICKS - (overworldTime - getLastUsed(stack))) / 20);
    }

    public static boolean isBroken(ItemStack stack) {
        return getOverheat(stack) >= MAX_OVERHEAT;
    }

    public static int calcFuelCost(double dist) {
        // 基础5 + 线性增长(每50格1点) + 二次项(每1000万格² 1点)
        // 短距离便宜，长距离急剧上升
        double base = 5.0 + dist / 50.0 + (dist * dist) / 10_000_000.0;
        return (int) Math.ceil(base);
    }

    public static int calcOverheat(double dist) {
        // 线性项极缓(每800格1点) + 二次项主导(每400万格² 1点)
        // 短距离几乎不涨，长距离过热爆炸，形成软天花板
        double base = dist / 800.0 + (dist * dist) / 4_000_000.0;
        return (int) Math.ceil(base);
    }

    public static ItemStack findInHands(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.isOf(items.VORTEX_MANIPULATOR)) return main;
        ItemStack off = player.getOffHandStack();
        return off.isOf(items.VORTEX_MANIPULATOR) ? off : ItemStack.EMPTY;
    }

    public static void saveCurrentAsDest(PlayerEntity player, ItemStack stack) {
        setDestX(stack, player.getX());
        setDestY(stack, player.getY());
        setDestZ(stack, player.getZ());
        setDestDim(stack, player.getWorld().getRegistryKey().getValue().toString());
    }

    public static void swapDestWithPrev(ItemStack stack) {
        double tx = getDestX(stack), ty = getDestY(stack), tz = getDestZ(stack);
        String td = getDestDim(stack);
        setDestX(stack, getPrevX(stack));
        setDestY(stack, getPrevY(stack));
        setDestZ(stack, getPrevZ(stack));
        setDestDim(stack, getPrevDim(stack));
        setPrevX(stack, tx);
        setPrevY(stack, ty);
        setPrevZ(stack, tz);
        setPrevDim(stack, td);
    }

    private static Text getDimensionDisplayText(String dimId) {
        if (dimId == null || dimId.isEmpty()) return Text.literal("-");
        try {
            Identifier id = new Identifier(dimId);
            String key = "dimension." + id.getNamespace() + "." + id.getPath();
            Text text = Text.translatable(key);
            if (text.getString().equals(key)) return Text.literal(dimId);
            return text;
        } catch (Exception e) {
            return Text.literal(dimId);
        }
    }
}