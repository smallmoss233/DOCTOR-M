package doctor_m.Item.data_itme;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import doctor_m.Item.KeytoTime;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.util.creativity.DynamicColorHelper;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class KeytoTimeItem extends TrinketItem implements KeytoTime {

    private static final String TITLE_KEY = "CustomTitle";

    public KeytoTimeItem(Settings settings) {
        super(settings);
    }

    /** 从饰品Stack读取称号，没有返回null */
    public static String getTitle(ItemStack stack) {
        if (stack.isEmpty()) return null;
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(TITLE_KEY)) {
            return nbt.getString(TITLE_KEY);
        }
        return null;
    }

    /** 写入/清除称号到饰品Stack */
    public static void setTitle(ItemStack stack, @Nullable String title) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (title == null || title.isBlank()) {
            nbt.remove(TITLE_KEY);
        } else {
            nbt.putString(TITLE_KEY, title);
        }
    }

    /** 从玩家Trinkets栏查找KeytoTime饰品并读取称号 */
    public static String getTitleFromPlayer(PlayerEntity player) {
        if (player == null) return null;
        var optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isEmpty()) return null;

        for (var pair : optional.get().getEquipped(s -> s.getItem() instanceof KeytoTimeItem)) {
            String title = getTitle(pair.getRight());
            if (title != null && !title.isBlank()) {
                return title;
            }
        }
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Text longDescription = Text.translatable("message.doctor_m.key_to_time.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.key_to_time.detail")
        );
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        if (entity instanceof PlayerEntity player) {
            if (!player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = true;
                player.sendAbilitiesUpdate();
            }
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onUnequip(stack, slot, entity);

        if (entity instanceof ServerPlayerEntity serverPlayer) {
            KeytoTimeCore.clearProtection(serverPlayer);
        }

        if (entity instanceof PlayerEntity player) {
            if (!player.isCreative() && player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.sendAbilitiesUpdate();
            }
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        List<Color> colors = List.of(
                new Color(128, 0, 128),
                Color.WHITE,
                Color.RED,
                Color.WHITE
        );
        return DynamicColorHelper.applyColorCycle(baseName, colors, 15000);
    }
}