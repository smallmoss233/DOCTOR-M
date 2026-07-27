package doctor_m.network;

import doctor_m.Item.data_weapon.de_mat_gun;
import doctor_m.util.EntityEraser;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class DeMatGunNetwork {
    public static final Identifier DE_MAT_SHOOT = new Identifier("doctor_m", "de_mat_shoot");

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(DE_MAT_SHOOT, (server, player, handler, buf, responseSender) -> {
            boolean isAds = buf.readBoolean();
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!(stack.getItem() instanceof de_mat_gun)) return;
                de_mat_gun gun = (de_mat_gun) stack.getItem();

                if (player.getItemCooldownManager().isCoolingDown(gun)) return;

                if (gun.getCurrentAmmo(stack) <= 0) {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return;
                }

                double cost = isAds ? 2 : 1;
                double current = gun.getCurrentAmmo(stack);
                gun.setCurrentAmmo(Math.max(0, current - cost), stack);

                player.getItemCooldownManager().set(gun, gun.getCooldown());

                EntityEraser.eraseByRaycast(player, player.getWorld());
            });
        });
    }
}