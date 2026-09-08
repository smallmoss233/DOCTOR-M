package doctor_m.network;

import doctor_m.module.creativity.creativity_data.STCS.STCS;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class STCSNetworking {
    public static final Identifier STCS_SKILL_ID = new Identifier("doctor_m", "stcs_skill");
    public static final Identifier STCS_CORE_ID = new Identifier("doctor_m", "stcs_core");

    public static void register() {
        // 二技能发包
        ServerPlayNetworking.registerGlobalReceiver(STCS_SKILL_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!(stack.getItem() instanceof STCS)) {
                    stack = player.getOffHandStack();
                }
                if (stack.getItem() instanceof STCS stcsItem) {
                    stcsItem.onSkillPressed(player, stack);
                }
            });
        });

        // 剑核心发包
        ServerPlayNetworking.registerGlobalReceiver(STCS_CORE_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!(stack.getItem() instanceof STCS)) {
                    stack = player.getOffHandStack();
                }
                if (stack.getItem() instanceof STCS stcsItem) {
                    stcsItem.onCorePressed(player, stack);
                }
            });
        });
    }
}