package doctor_m.mixin.sonic_mode;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.amble.ait.core.item.SonicItem;
import dev.amble.ait.core.item.sonic.SonicMode;
import dev.amble.ait.core.item.sonic.TardisSonicMode;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.lib.data.CachedDirectedGlobalPos;

@Mixin(TardisSonicMode.class)
public abstract class TardisSonicModeMixin extends SonicMode {

    protected TardisSonicModeMixin(int index) {
        super(index);
    }

    @Unique
    private Text doctor_m$appendText = null;
    @Unique
    private boolean doctor_m$shouldAppend = false;

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void doctor_m$onInteractBlockHead(ItemStack stack, World world, ServerPlayerEntity player, BlockPos pos,
                                              CallbackInfoReturnable<Boolean> cir) {
        this.doctor_m$appendText = null;
        this.doctor_m$shouldAppend = false;

        if (isPlayerInTardisDimension(player)) return;

        Tardis tardis = SonicItem.getTardisStatic(world, stack);
        if (tardis == null) return;

        CachedDirectedGlobalPos tardisPos = tardis.travel().position();
        if (tardisPos == null) return;

        this.doctor_m$shouldAppend = true;

        BlockPos playerBlockPos = player.getBlockPos();
        String playerDimension = player.getWorld().getRegistryKey().getValue().toString();

        BlockPos tardisBlockPos = tardisPos.getPos();
        String tardisDimension = tardisPos.getWorld().getRegistryKey().getValue().toString();

        boolean sameDimension = player.getWorld().getRegistryKey().equals(tardisPos.getWorld().getRegistryKey());

        Text playerLoc = Text.translatable("tooltip.doctor_m.sonic.player_location",
                        playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ(), playerDimension)
                .formatted(Formatting.GREEN);

        if (sameDimension) {
            double dx = tardisBlockPos.getX() - playerBlockPos.getX();
            double dz = tardisBlockPos.getZ() - playerBlockPos.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            String arrow = getRelativeDirectionArrow(player, dx, dz);

            Text tardisLoc = Text.translatable("tooltip.doctor_m.sonic.tardis_location_with_arrow",
                            tardisBlockPos.getX(), tardisBlockPos.getY(), tardisBlockPos.getZ(),
                            (int) Math.round(distance), arrow)
                    .formatted(Formatting.GOLD);

            this.doctor_m$appendText = Text.empty()
                    .append(playerLoc)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(tardisLoc);
        } else {
            Text diffWorld = Text.translatable("tooltip.doctor_m.sonic.different_world", tardisDimension)
                    .formatted(Formatting.RED);

            this.doctor_m$appendText = Text.empty()
                    .append(playerLoc)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(diffWorld);
        }
    }

    @ModifyArg(
            method = "interactBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V"
            ),
            index = 0
    )
    private Text doctor_m$mergeMessage(Text original) {
        // 成功召唤时不附加位置信息
        if (this.doctor_m$isSuccessMessage(original)) {
            return original;
        }

        if (this.doctor_m$shouldAppend && this.doctor_m$appendText != null) {
            return Text.empty()
                    .append(original)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(this.doctor_m$appendText);
        }
        return original;
    }

    @Unique
    private boolean doctor_m$isSuccessMessage(Text text) {
        // Text.translatable 在服务端 toString() 仍包含翻译键，跨 mappings 兼容
        return text.toString().contains("sonic.ait.mode.tardis.location_summon");
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void doctor_m$onInteractBlockReturn(ItemStack stack, World world, ServerPlayerEntity player, BlockPos pos,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        if (isPlayerInTardisDimension(player)) return;

        Tardis tardis = SonicItem.getTardisStatic(world, stack);
        if (tardis == null) {
            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.sonic.tardis_not_bound").formatted(Formatting.RED),
                    true
            );
            return;
        }

        CachedDirectedGlobalPos tardisPos = tardis.travel().position();
        if (tardisPos == null) {
            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.sonic.position_unknown").formatted(Formatting.RED),
                    true
            );
        }
    }

    private boolean isPlayerInTardisDimension(ServerPlayerEntity player) {
        String dimId = player.getWorld().getRegistryKey().getValue().toString();
        return dimId.startsWith("ait-tardis:");
    }

    private String getRelativeDirectionArrow(ServerPlayerEntity player, double dx, double dz) {
        double targetAngle = Math.atan2(dx, dz);
        double playerYawRad = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double diff = targetAngle + playerYawRad;

        while (diff < -Math.PI) diff += 2 * Math.PI;
        while (diff > Math.PI) diff -= 2 * Math.PI;

        double degrees = diff * MathHelper.DEGREES_PER_RADIAN;

        if (degrees < -157.5 || degrees >= 157.5) return "↓";
        else if (degrees < -112.5) return "↘";
        else if (degrees < -67.5) return "→";
        else if (degrees < -22.5) return "↗";
        else if (degrees < 22.5) return "↑";
        else if (degrees < 67.5) return "↖";
        else if (degrees < 112.5) return "←";
        else return "↙";
    }
}