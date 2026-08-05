package doctor_m.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class TimeKeyTeleportNetwork {
    public static final Identifier TELEPORT = new Identifier("doctor_m", "time_key_teleport");

    public static void register() {
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

                // 直接传送，无燃料/过热/损坏/维度锁定限制
                player.teleport(targetWorld, x, y, z, player.getYaw(), player.getPitch());
            });
        });
    }
}