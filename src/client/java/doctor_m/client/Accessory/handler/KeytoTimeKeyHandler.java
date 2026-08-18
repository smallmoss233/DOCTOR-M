package doctor_m.client.Accessory.handler;

import doctor_m.client.Accessory.AccessoryKeyHandler;
import doctor_m.client.gui.KeytoTimeActiveScreen;
import doctor_m.client.gui.KeytoTimePassiveScreen;
import doctor_m.handler.KeytoTime.KeytoTimeCore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class KeytoTimeKeyHandler implements AccessoryKeyHandler {

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isActive(PlayerEntity player) {
        return KeytoTimeCore.isTimeKeyEquipped(player);
    }

    @Override
    public void onSkillKey(PlayerEntity player) {
        MinecraftClient.getInstance().setScreen(new KeytoTimePassiveScreen(player));
    }

    @Override
    public void onCoreKey(PlayerEntity player) {
        MinecraftClient.getInstance().setScreen(new KeytoTimeActiveScreen(player));
    }

    @Override
    public boolean blocksOthers() {
        return true;
    }
}