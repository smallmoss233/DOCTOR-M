package doctor_m.mixin.client.ait;

import dev.amble.ait.core.AITStatusEffects;
import dev.amble.ait.core.world.TardisServerWorld;
import dev.amble.ait.module.planet.client.SpaceSuitOverlay;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import dev.amble.ait.module.planet.core.space.planet.PlanetRegistry;
import doctor_m.module.space_plus.OxygenSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpaceSuitOverlay.class)
public class MixinSpaceSuitOverlay {

    @Inject(method = "onHudRender", at = @At("HEAD"), cancellable = true)
    private void onHudRender(DrawContext drawContext, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (!mc.options.getPerspective().isFirstPerson()) return;
        if (!(mc.player.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof SpacesuitItem)) return;

        renderCustomHud(drawContext);
        ci.cancel();
    }

    private void renderCustomHud(DrawContext drawContext) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        var chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        double oxygen = OxygenSystem.getOxygen(chestStack);

        // 与 MixinSpacesuitItem 逻辑一致
        boolean worldHasOxygen = true;
        try {
            var planet = PlanetRegistry.getInstance().get(mc.world);
            if (planet != null) {
                worldHasOxygen = planet.hasOxygen();
            }
        } catch (Exception ignored) {}

        boolean isTardis = TardisServerWorld.isTardisDimension(mc.world);
        boolean hasOxygenated = mc.player.hasStatusEffect(AITStatusEffects.OXYGENATED);

        boolean isSubmerged = mc.player.isSubmergedInWater();

        // ========== 修复：头部窒息判定 ==========
        BlockPos eyePos = BlockPos.ofFloored(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        BlockState headState = mc.world.getBlockState(eyePos);
        boolean isHeadInsideBlock = headState.isSolidBlock(mc.world, eyePos);
        // =======================================

        boolean isUnbreathableEnvironment = isSubmerged || isHeadInsideBlock;

        boolean hasOxygen = (worldHasOxygen || isTardis || hasOxygenated) && !isUnbreathableEnvironment;

        Text envText = hasOxygen ?
                Text.translatable("hud.doctor_m.environment.oxygenated").formatted(Formatting.GREEN) :
                Text.translatable("hud.doctor_m.environment.deoxygenated").formatted(Formatting.RED);
        drawContext.drawTextWithShadow(textRenderer, envText, 2, 2, 0xFFFFFF);

        String oxygenText = String.format("%.1f", oxygen) + "L / " + OxygenSystem.getMaxOxygen() + "L";
        drawContext.drawTextWithShadow(textRenderer,
                Text.literal(oxygenText),
                2, 40, 0xFFFFFF);
    }
}