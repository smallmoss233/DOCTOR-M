package doctor_m.module.creativity.creativity_data;

import com.google.common.collect.Multimap;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
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

import doctor_m.util.creativity.ScytheSlashManager;

public class tlipoca_scythe extends SwordItem {
    private static final String INIT_KEY = "TlipocaInit";

    private static final UUID DAMAGE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789014");
    private static final UUID SPEED_UUID = UUID.fromString("12345678-1234-1234-1234-123456789016");
    public static final UUID TLIPOCA_HEALTH_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    // 单例引用（用于冷却管理）
    private static tlipoca_scythe INSTANCE;
    public static tlipoca_scythe getInstance() { return INSTANCE; }

    public tlipoca_scythe(Settings settings) {
        super(TlipocaMaterial.INSTANCE, 0, -3.2f, settings);
        INSTANCE = this;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return super.getAttributeModifiers(slot);
    }
    /**
     * 写入 AttributeModifiers（固定伤害 20，速度 -3.6）
     */
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

        nbt.put("AttributeModifiers", list);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            if (user instanceof ServerPlayerEntity serverPlayer) {
                // ⭐ 传入 stack 作为第三个参数
                ScytheSlashManager.performSlash((net.minecraft.server.world.ServerWorld) world, serverPlayer, stack);
            }
        } else {
            ScytheSlashManager.spawnParticlesClient(user);
        }

        return TypedActionResult.success(stack);
    }

    // ========== Tooltip ==========

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        Text longDescription = Text.translatable("message.doctor_m.tlipoca_scythe.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.tlipoca_scythe.detail")
        );
    }
    @Override
    public boolean isDamageable() {
        return false;
    }
}