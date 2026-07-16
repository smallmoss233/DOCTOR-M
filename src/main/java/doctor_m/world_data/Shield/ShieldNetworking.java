package doctor_m.world_data.Shield;  // 或者你项目里合适的包

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ShieldNetworking {
    public static final Identifier SHIELD_ACTIVATION = new Identifier("doctor_m", "shield_activation");

    // 只在服务端调用，发送给指定玩家
    public static void sendShieldActivation(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, SHIELD_ACTIVATION, PacketByteBufs.empty());
    }
}