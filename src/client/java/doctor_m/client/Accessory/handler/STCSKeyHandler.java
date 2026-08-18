package doctor_m.client.Accessory.handler;

import doctor_m.Item.stcs.STCSItem;
import doctor_m.client.Accessory.AccessoryKeyHandler;
import doctor_m.network.STCSNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;

public class STCSKeyHandler implements AccessoryKeyHandler {

    @Override
    public int getPriority() {
        return 10; // 高优先级，优先于 TimeKey
    }

    @Override
    public boolean isActive(PlayerEntity player) {
        // STCS 只认主手和副手
        return player.getMainHandStack().getItem() instanceof STCSItem
                || player.getOffHandStack().getItem() instanceof STCSItem;
    }

    @Override
    public void onSkillKey(PlayerEntity player) {
        ClientPlayNetworking.send(STCSNetworking.STCS_SKILL_ID, PacketByteBufs.create());
    }

    @Override
    public void onCoreKey(PlayerEntity player) {
        ClientPlayNetworking.send(STCSNetworking.STCS_CORE_ID, PacketByteBufs.create());
    }

    @Override
    public boolean blocksOthers() {
        return true; // STCS 占用按键，不往下传
    }
}