package doctor_m.handler.KeytoTime;

import doctor_m.util.creativity.ScytheSlashManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class KeytoTimePassive {

    private static final String GODMODE_KEY = "godmode";
    private static final String NEUTRAL_KEY = "neutral_mode";
    private static final String SLASH_KEY = "slash_mode";

    public static void toggleFeature(PlayerEntity player, int featureId) {
        ItemStack stack = KeytoTimeCore.getTimeKeyStack(player);
        if (stack.isEmpty()) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        String key;
        switch (featureId) {
            case 0 -> key = GODMODE_KEY;
            case 1 -> key = NEUTRAL_KEY;
            case 2 -> key = SLASH_KEY;
            default -> { return; }
        }

        boolean current = nbt.getBoolean(key);
        nbt.putBoolean(key, !current);

        String msgKey;
        switch (featureId) {
            case 0 -> msgKey = "gui.doctor_m.key_to_time.godmode";
            case 1 -> msgKey = "gui.doctor_m.key_to_time.neutral_mode";
            case 2 -> msgKey = "gui.doctor_m.key_to_time.slash_mode";
            default -> { return; }
        }

        player.sendMessage(Text.translatable(msgKey + "." + (current ? "off" : "on")), true);
    }

    public static boolean isGodMode(PlayerEntity player) {
        ItemStack stack = KeytoTimeCore.getTimeKeyStack(player);
        return !stack.isEmpty() && stack.getOrCreateNbt().getBoolean(GODMODE_KEY);
    }

    public static boolean isNeutralMode(PlayerEntity player) {
        ItemStack stack = KeytoTimeCore.getTimeKeyStack(player);
        return !stack.isEmpty() && stack.getOrCreateNbt().getBoolean(NEUTRAL_KEY);
    }

    public static boolean isSlashMode(PlayerEntity player) {
        ItemStack stack = KeytoTimeCore.getTimeKeyStack(player);
        return !stack.isEmpty() && stack.getOrCreateNbt().getBoolean(SLASH_KEY);
    }

    public static void registerAttackCallback() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity)) return ActionResult.PASS;
            if (!KeytoTimeCore.isTimeKeyEquipped(serverPlayer)) return ActionResult.PASS;
            if (!isSlashMode(serverPlayer)) return ActionResult.PASS;

            ScytheSlashManager.performSlashEffect(serverPlayer.getServerWorld(), serverPlayer, 5);
            return ActionResult.PASS;
        });
    }
}