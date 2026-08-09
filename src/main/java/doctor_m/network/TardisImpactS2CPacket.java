package doctor_m.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class TardisImpactS2CPacket {
    public static final Identifier ID = new Identifier("doctor_m", "tardis_impact");

    public static void write(PacketByteBuf buf, Vec3d pos, float intensity) {
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeFloat(intensity);
    }

    /** 服务端/客户端通用：读出 shake 强度。调用前 buf 指针必须在开头。 */
    public static float read(PacketByteBuf buf) {
        buf.readDouble(); // skip x
        buf.readDouble(); // skip y
        buf.readDouble(); // skip z
        return buf.readFloat();
    }
}