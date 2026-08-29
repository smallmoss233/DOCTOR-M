package doctor_m.network;

import dev.amble.ait.core.util.WorldUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class KeytoTimeTeleportNetwork {
    public static final Identifier TELEPORT = new Identifier("doctor_m", "key_to_time_teleport");
    public static final Identifier REQUEST_DIMS = new Identifier("doctor_m", "key_to_time_request_dims");
    public static final Identifier DIMS_RESPONSE = new Identifier("doctor_m", "key_to_time_dims_response");

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_DIMS, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                List<String> dims = new ArrayList<>();
                for (var world : WorldUtil.getTravelWorlds()) {
                    String id = world.getRegistryKey().getValue().toString();
                    if (!id.startsWith("ait-tardis:")) {
                        dims.add(id);
                    }
                }

                PacketByteBuf response = PacketByteBufs.create();
                response.writeInt(dims.size());
                for (String dim : dims) {
                    response.writeString(dim);
                }
                responseSender.sendPacket(DIMS_RESPONSE, response);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TELEPORT, (server, player, handler, buf, responseSender) -> {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            String dimId = buf.readString();

            server.execute(() -> {
                RegistryKey<World> targetKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimId));
                var targetWorld = server.getWorld(targetKey);
                if (targetWorld == null) {
                    player.sendMessage(
                            Text.translatable("message.doctor_m.vm.invalid_dimension")
                                    .formatted(Formatting.RED),
                            true
                    );
                    return;
                }
                player.teleport(targetWorld, x, y, z, player.getYaw(), player.getPitch());
            });
        });
    }
}