package doctor_m.module.creativity.creativity_data.Tlipoca;

import doctor_m.util.creativity.DynamicColorHelper;
import doctor_m.util.creativity.ScytheChargingManager;
import doctor_m.util.creativity.ScytheSlashManager;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class TlipocaScytheItem extends Item {
    private static final String INIT_KEY = "TlipocaInit";

    private static final UUID DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789014");
    private static final UUID SPEED_UUID = UUID.fromString("12345678-1234-1234-1234-123456789016");
    private static final UUID REACH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789017");

    private static TlipocaScytheItem INSTANCE;
    public static TlipocaScytheItem getInstance() { return INSTANCE; }

    public TlipocaScytheItem(Settings settings) {
        super(settings.maxCount(1));
        INSTANCE = this;
    }

    public static void writeAttributeModifiers(ItemStack stack, float damage) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList list = new NbtList();

        NbtCompound dmg = new NbtCompound();
        dmg.putString("AttributeName", "minecraft:generic.attack_damage");
        dmg.putString("Name", "tlipoca_damage");
        dmg.putDouble("Amount", damage);
        dmg.putInt("Operation", 0);
        dmg.putUuid("UUID", DAMAGE_UUID);
        dmg.putString("Slot", "mainhand");
        list.add(dmg);

        NbtCompound spd = new NbtCompound();
        spd.putString("AttributeName", "minecraft:generic.attack_speed");
        spd.putString("Name", "tlipoca_speed");
        spd.putDouble("Amount", -3.6);
        spd.putInt("Operation", 0);
        spd.putUuid("UUID", SPEED_UUID);
        spd.putString("Slot", "mainhand");
        list.add(spd);

        NbtCompound reach = new NbtCompound();
        reach.putString("AttributeName", "minecraft:player.entity_interaction_range");
        reach.putString("Name", "tlipoca_reach");
        reach.putDouble("Amount", 1.5);
        reach.putInt("Operation", 0);
        reach.putUuid("UUID", REACH_UUID);
        reach.putString("Slot", "mainhand");
        list.add(reach);

        nbt.put("AttributeModifiers", list);
    }

    // ========== 多层蓄力斩击 ==========

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);

        ScytheChargingManager.startCharging(user);

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            world.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.5f, 1.8f);
        }
        return TypedActionResult.consume(stack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient || !(user instanceof PlayerEntity player)) return;

        int useTicks = this.getMaxUseTime(stack) - remainingUseTicks;
        int level = Math.min(useTicks / ScytheChargingManager.TICKS_PER_LEVEL,
                ScytheChargingManager.MAX_CHARGE_LEVEL);

        int prevLevel = ScytheChargingManager.getChargeLevel(player);
        if (level != prevLevel) {
            ScytheChargingManager.setChargeLevel(player, level);
            if (level > 0) {
                player.sendMessage(
                        Text.translatable("message.doctor_m.scythe.charging_level", level, ScytheChargingManager.MAX_CHARGE_LEVEL)
                                .formatted(Formatting.DARK_RED, Formatting.BOLD),
                        true
                );
                ScytheSlashManager.spawnLevelUpParticlesClient(player, level);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.5f - level * 0.12f);
            }
        }

        ScytheSlashManager.spawnChargeParticlesClient(player, useTicks);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) return;

        ScytheChargingManager.stopCharging(player);

        int useTicks = this.getMaxUseTime(stack) - remainingUseTicks;
        int level = Math.min(useTicks / ScytheChargingManager.TICKS_PER_LEVEL,
                ScytheChargingManager.MAX_CHARGE_LEVEL);

        if (level <= 0) return;

        ScytheSlashManager.performChargedSlash(
                (net.minecraft.server.world.ServerWorld) world, player, stack, level);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    // ========== Tooltip ==========

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        Text longDescription = Text.translatable("message.doctor_m.tlipoca_scythe.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.tlipoca_scythe.detail"));
    }

    // ========== 初始化属性 ==========

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.getBoolean(INIT_KEY)) {
            nbt.putBoolean(INIT_KEY, true);
            writeAttributeModifiers(stack, 20.0f);
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(255, 0, 0),
                new Color(255, 0, 0),
                new Color(0, 0, 0)
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 30000);
    }
}