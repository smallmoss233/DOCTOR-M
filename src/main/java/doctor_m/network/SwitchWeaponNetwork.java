package doctor_m.network;

import doctor_m.module.creativity.creativity_data.SAR.SAR;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class SwitchWeaponNetwork {

    public static final Identifier SWITCH_WEAPON_PACKET = new Identifier("doctor_m", "switch_weapon_sector");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SWITCH_WEAPON_PACKET, (server, player, handler, buf, responseSender) -> {
            int sector = buf.readInt();

            server.execute(() -> {
                ItemStack mainHand = player.getMainHandStack();
                if (mainHand.getItem() instanceof SAR sar) {
                    ItemStack newStack = sar.getSwitchedItem(player, mainHand, sector);
                    if (newStack != null && !newStack.isEmpty() && newStack.getItem() != mainHand.getItem()) {
                        player.setStackInHand(Hand.MAIN_HAND, newStack);
                    }
                }
            });
        });
    }
}