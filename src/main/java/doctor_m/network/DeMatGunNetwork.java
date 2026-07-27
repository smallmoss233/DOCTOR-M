package doctor_m.network;

import doctor_m.Item.data_weapon.DeMatGunItem;
import doctor_m.world_data.DeMatGunEntityEraser;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
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
                if (!(stack.getItem() instanceof DeMatGunItem)) return;
                DeMatGunItem gun = (DeMatGunItem) stack.getItem();

                if (player.getItemCooldownManager().isCoolingDown(gun)) return;

                if (gun.getCurrentAmmo(stack) <= 0) {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return;
                }

                double cost = isAds ? 2 : 1;
                double current = gun.getCurrentAmmo(stack);
                gun.setCurrentAmmo(Math.max(0, current - cost), stack);

                double range = isAds ? 256.0 : 128.0;
                DeMatGunEntityEraser.eraseByRaycast(player, player.getWorld(), range);

                player.getItemCooldownManager().set(gun, gun.getCooldown());
            });
        });
    }
}