package doctor_m.network;

import doctor_m.module.creativity.creativity_data.SAR.SAR;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class SARNetworking {
    public static final Identifier SAR_SKILL_ID = new Identifier("doctor_m", "sar_skill");
    public static final Identifier SAR_CORE_ID = new Identifier("doctor_m", "sar_core");

    public static void register() {
        // 二技能发包
        ServerPlayNetworking.registerGlobalReceiver(SAR_SKILL_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!(stack.getItem() instanceof SAR)) {
                    stack = player.getOffHandStack();
                }
                if (stack.getItem() instanceof SAR stcsItem) {
                    stcsItem.onSkillPressed(player, stack);
                }
            });
        });

        // 剑核心发包
        ServerPlayNetworking.registerGlobalReceiver(SAR_CORE_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!(stack.getItem() instanceof SAR)) {
                    stack = player.getOffHandStack();
                }
                if (stack.getItem() instanceof SAR stcsItem) {
                    stcsItem.onCorePressed(player, stack);
                }
            });
        });
    }
}