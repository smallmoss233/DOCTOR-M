package doctor_m.network;

import doctor_m.menu.ATCLModificationSystemScreenHandler;
import doctor_m.module.creativity.creativity_data.SAR.SAR;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ATCLModificationSystemNetwork {
    public static final Identifier ID = new Identifier("doctor_m", "open_atcl_modification_system");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!(player.getMainHandStack().getItem() instanceof SAR)) {
                    // 改成了读取翻译键
                    player.sendMessage(Text.translatable("message.doctor_m.atcl.require_sar").formatted(Formatting.RED), true);
                    return;
                }
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, p) -> new ATCLModificationSystemScreenHandler(syncId, inv),
                        Text.translatable("gui.doctor_m.atcl_modification_system")
                ));
            });
        });
    }
}