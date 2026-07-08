package com.example.doctor_m.mixin.SonicMode;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(
            method = "interactBlock",
            at = @At("RETURN"),
            cancellable = false
    )
    private void onInteractBlockReturn(ItemStack stack, World world, ServerPlayerEntity player, BlockPos pos,
                                       CallbackInfoReturnable<Boolean> cir) {

        boolean isMainHand = player.getMainHandStack() == stack;
        if (!isMainHand) return;

        if (cir.getReturnValue()) return;

        // ===== 关键：玩家当前在 TARDIS 维度内 → 直接 return，不显示任何定位消息 =====
        if (isPlayerInTardisDimension(player)) {
            return;
        }

        Tardis tardis = SonicItem.getTardisStatic(world, stack);
        if (tardis == null) {
            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.sonic.tardis_not_bound")
                            .formatted(Formatting.RED),
                    false
            );
            return;
        }

        CachedDirectedGlobalPos tardisPos = tardis.travel().position();
        if (tardisPos == null) {
            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.sonic.position_unknown")
                            .formatted(Formatting.RED),
                    false
            );
            return;
        }

        BlockPos playerBlockPos = player.getBlockPos();
        String playerDimension = player.getWorld().getRegistryKey().getValue().toString();

        BlockPos tardisBlockPos = tardisPos.getPos();
        String tardisDimension = tardisPos.getWorld().getRegistryKey().getValue().toString();

        boolean sameDimension = player.getWorld().getRegistryKey().equals(tardisPos.getWorld().getRegistryKey());

        // 玩家位置
        player.sendMessage(
                Text.translatable("tooltip.doctor_m.sonic.player_location",
                                playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ(), playerDimension)
                        .formatted(Formatting.GREEN),
                false
        );

        if (sameDimension) {
            double dx = tardisBlockPos.getX() - playerBlockPos.getX();
            double dz = tardisBlockPos.getZ() - playerBlockPos.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            String arrow = getRelativeDirectionArrow(player, dx, dz);

            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.sonic.tardis_location_with_arrow",
                                    tardisBlockPos.getX(), tardisBlockPos.getY(), tardisBlockPos.getZ(),
                                    (int) Math.round(distance),
                                    arrow)
                            .formatted(Formatting.GOLD),
                    false
            );
        } else {
            player.sendMessage(
                    Text.translatable("tooltip.doctor_m.sonic.different_world",
                                    tardisDimension)
                            .formatted(Formatting.RED),
                    false
            );
        }
    }

    /**
     * 判断玩家当前是否处于 TARDIS 内部维度
     * 格式：ait-tardis:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */
    private boolean isPlayerInTardisDimension(ServerPlayerEntity player) {
        String dimId = player.getWorld().getRegistryKey().getValue().toString();
        return dimId.startsWith("ait-tardis:");
    }

    /**
     * 计算相对于玩家视角的方向引导箭头
     */
    private String getRelativeDirectionArrow(ServerPlayerEntity player, double dx, double dz) {
        double targetAngle = Math.atan2(dx, dz);
        double playerYawRad = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double diff = targetAngle + playerYawRad;

        while (diff < -Math.PI) diff += 2 * Math.PI;
        while (diff > Math.PI) diff -= 2 * Math.PI;

        double degrees = diff * MathHelper.DEGREES_PER_RADIAN;

        if (degrees < -157.5 || degrees >= 157.5) {
            return "↓";
        } else if (degrees < -112.5) {
            return "↘";
        } else if (degrees < -67.5) {
            return "→";
        } else if (degrees < -22.5) {
            return "↗";
        } else if (degrees < 22.5) {
            return "↑";
        } else if (degrees < 67.5) {
            return "↖";
        } else if (degrees < 112.5) {
            return "←";
        } else {
            return "↙";
        }
    }
}