package doctor_m.client.network;

import doctor_m.network.DeMatGunNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

@Environment(EnvType.CLIENT)
public class DeMatGunClientNetwork {
    public static void sendShootPacket(boolean isAds) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isAds);
        ClientPlayNetworking.send(DeMatGunNetwork.DE_MAT_SHOOT, buf);
    }
}