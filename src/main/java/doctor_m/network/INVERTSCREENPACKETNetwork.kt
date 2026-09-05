package doctor_m.network

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

object INVERTSCREENPACKETNetwork {
    @JvmField
    val INVERT_SCREEN_PACKET: Identifier = Identifier("doctor_m", "invert_screen")

    fun sendInvertScreenPacket(player: ServerPlayerEntity, durationTicks: Int) {
        val buf = PacketByteBufs.create()
        buf.writeInt(durationTicks)
        ServerPlayNetworking.send(player, INVERT_SCREEN_PACKET, buf)
    }
}