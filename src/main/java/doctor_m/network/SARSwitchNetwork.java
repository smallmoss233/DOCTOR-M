package doctor_m.network;

import doctor_m.module.creativity.CreativityItems;
import doctor_m.module.creativity.creativity_data.SAR.SARAItem;
import doctor_m.module.creativity.creativity_data.SAR.SARHItem;
import doctor_m.module.creativity.creativity_data.SAR.SARLItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SARSwitchNetwork {
    public static final Identifier ID = new Identifier("doctor_m", "sar_switch");

    public static final byte TARGET_I = 1;
    public static final byte TARGET_U = 2;

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            byte target = buf.readByte();
            server.execute(() -> handle(player, target));
        });
    }

    private static void handle(ServerPlayerEntity player, byte target) {
        var inv = player.getInventory();
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        if (mainHand.getItem() instanceof SARAItem || mainHand.getItem() instanceof SARLItem || mainHand.getItem() instanceof SARHItem) {
            ItemStack newStack = getSwitchedStack(mainHand, target);
            if (newStack != null) inv.setStack(inv.selectedSlot, newStack);
        } else if (offHand.getItem() instanceof SARAItem || offHand.getItem() instanceof SARLItem || offHand.getItem() instanceof SARHItem) {
            ItemStack newStack = getSwitchedStack(offHand, target);
            if (newStack != null) inv.offHand.set(0, newStack);
        }
    }

    private static ItemStack getSwitchedStack(ItemStack oldStack, byte target) {
        Item targetItem = null;

        if (oldStack.getItem() instanceof SARAItem) {
            targetItem = target == TARGET_I ? CreativityItems.SARL : CreativityItems.SARH;
        } else if (oldStack.getItem() instanceof SARLItem && target == TARGET_I) {
            targetItem = CreativityItems.SARA;
        } else if (oldStack.getItem() instanceof SARHItem && target == TARGET_U) {
            targetItem = CreativityItems.SARA;
        }

        if (targetItem != null) {
            ItemStack newStack = new ItemStack(targetItem);
            if (oldStack.hasNbt() && oldStack.getNbt() != null) {
                newStack.setNbt(oldStack.getNbt().copy());
            }
            return newStack;
        }
        return null;
    }
}