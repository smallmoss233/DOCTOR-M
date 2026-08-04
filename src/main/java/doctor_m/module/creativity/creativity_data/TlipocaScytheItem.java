package doctor_m.module.creativity.creativity_data;

import com.google.common.collect.Multimap;
import doctor_m.util.creativity.ScytheSlashManager;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class TlipocaScytheItem extends SwordItem {
    private static final String INIT_KEY = "TlipocaInit";

    private static final UUID DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789014");
    private static final UUID SPEED_UUID = UUID.fromString("12345678-1234-1234-1234-123456789016");
    private static final UUID REACH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789017");
    public static final UUID TLIPOCA_HEALTH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    private static TlipocaScytheItem INSTANCE;
    public static TlipocaScytheItem getInstance() { return INSTANCE; }

    public TlipocaScytheItem(Settings settings) {
        super(TlipocaMaterial.INSTANCE, 0, -3.2f, settings);
        INSTANCE = this;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return super.getAttributeModifiers(slot);
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

        // 新增：攻击距离 +1.5 格（镰刀比剑长）
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

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            if (user instanceof ServerPlayerEntity serverPlayer) {
                ScytheSlashManager.performSlash((net.minecraft.server.world.ServerWorld) world, serverPlayer, stack);
            }
        } else {
            ScytheSlashManager.spawnParticlesClient(user);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        Text longDescription = Text.translatable("message.doctor_m.tlipoca_scythe.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.tlipoca_scythe.detail"));
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
    }

    // ========== 彻底删除耐久系统 ==========

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return false;
    }

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
}