package doctor_m.network;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class UpdateObeliskPacket {

    public static final Identifier ID = new Identifier("doctor_m", "update_obelisk");

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            float yOffset = buf.readFloat();
            float scale = buf.readFloat();

            server.execute(() -> {
                if (player.getWorld().getBlockEntity(pos) instanceof EyeOfHarmonyObeliskBlockEntity obelisk) {
                    obelisk.setYOffset(yOffset);
                    obelisk.setScale(scale);
                }
            });
        });
    }

    public static PacketByteBuf createBuf(BlockPos pos, float yOffset, float scale) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeFloat(yOffset);
        buf.writeFloat(scale);
        return buf;
    }
}