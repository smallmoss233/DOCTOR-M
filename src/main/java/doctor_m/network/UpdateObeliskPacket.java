package doctor_m.network;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * 同步方尖碑 Y 轴偏移的网络包
 * 客户端拖动滑块后发送给服务器更新 BlockEntity
 */
public class UpdateObeliskPacket {

    public static final Identifier ID = new Identifier("doctor_m", "update_obelisk_y");

    /**
     * 在服务器初始化时调用，注册包处理器
     */
    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            float yOffset = buf.readFloat();

            server.execute(() -> {
                if (player.getWorld().getBlockEntity(pos) instanceof EyeOfHarmonyObeliskBlockEntity obelisk) {
                    obelisk.setYOffset(yOffset);
                }
            });
        });
    }

    /**
     * 创建数据包缓冲区
     */
    public static PacketByteBuf createBuf(BlockPos pos, float yOffset) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeFloat(yOffset);
        return buf;
    }
}