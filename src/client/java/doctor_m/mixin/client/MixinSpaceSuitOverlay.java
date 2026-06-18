package doctor_m.mixin.client;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.module.planet.client.SpaceSuitOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import doctor_m.module.space.SpaceOxygenManager;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;

@Mixin(SpaceSuitOverlay.class)
public class MixinSpaceSuitOverlay {

    @Inject(method = "onHudRender", at = @At("HEAD"), cancellable = true)
    private void onHudRender(DrawContext drawContext, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // 只处理第一人称且穿着宇航服头盔
        if (!mc.options.getPerspective().isFirstPerson()) return;
        if (!(mc.player.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof SpacesuitItem)) return;

        // 完全替换渲染逻辑
        renderCustomHud(drawContext);
        ci.cancel(); // 取消 AIT 原渲染
    }

    private void renderCustomHud(DrawContext drawContext) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        // 获取宇航服氧气值
        var chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        double oxygen = SpaceOxygenManager.getOxygen(chestStack);

        // 检测玩家是否在氧气机覆盖范围内（通过是否有 OXYGENATED 效果）
        boolean hasOxygen = mc.player.hasStatusEffect(AITStatusEffects.OXYGENATED);

        // 显示环境氧气状态
        Text envText = hasOxygen ?
                Text.translatable("hud.doctor_m.environment.oxygenated").formatted(Formatting.GREEN) :
                Text.translatable("hud.doctor_m.environment.deoxygenated").formatted(Formatting.RED);
        drawContext.drawTextWithShadow(textRenderer, envText, 2, 2, 0xFFFFFF);

        // 渲染宇航服氧气
        String oxygenText = String.format("%.1f", oxygen) + "L / " + SpaceOxygenManager.MAX_OXYGEN + "L";
        drawContext.drawTextWithShadow(textRenderer,
                Text.literal(oxygenText),
                2, 40, 0xFFFFFF);
    }
}