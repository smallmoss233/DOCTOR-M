package doctor_m.client.Accessory.handler;

import doctor_m.client.Accessory.AccessoryKeyHandler;
import doctor_m.module.creativity.creativity_data.SAR.SAR;
import doctor_m.network.SARNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;

public class SARKeyHandler implements AccessoryKeyHandler {

    @Override
    public int getPriority() {
        return 10; // 高优先级，优先于 TimeKey
    }

    @Override
    public boolean isActive(PlayerEntity player) {
        // SAR 只认主手和副手
        return player.getMainHandStack().getItem() instanceof SAR
                || player.getOffHandStack().getItem() instanceof SAR;
    }

    @Override
    public void onSkillKey(PlayerEntity player) {
        ClientPlayNetworking.send(SARNetworking.SAR_SKILL_ID, PacketByteBufs.create());
    }

    @Override
    public void onCoreKey(PlayerEntity player) {
        ClientPlayNetworking.send(SARNetworking.SAR_CORE_ID, PacketByteBufs.create());
    }

    @Override
    public boolean blocksOthers() {
        return true; // SAR 占用按键，不往下传
    }
}