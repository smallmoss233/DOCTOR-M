package doctor_m.module.creativity.creativity_data.SAR;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public interface SwitchableItem {
    ItemStack onSwitchI(ServerPlayerEntity player, ItemStack stack);
    ItemStack onSwitchU(ServerPlayerEntity player, ItemStack stack);
}