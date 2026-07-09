package com.example.doctor_m.mixin.SonicMode;

import dev.amble.ait.core.item.sonic.ScanningSonicMode;
import dev.amble.ait.core.item.sonic.SonicMode;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScanningSonicMode.class)
public abstract class ScanningSonicModeMixin extends SonicMode {

    protected ScanningSonicModeMixin(int index) {
        super(index);
    }

    // ========== 扫描方块：墙后空间探测 ==========
    @Inject(method = "scanBlocks", at = @At("RETURN"), cancellable = false)
    private void onScanBlocks(ItemStack stack, World world, PlayerEntity user, BlockPos pos,
                              CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient() || user == null) return;

        Vec3d eyePos = user.getCameraPosVec(1.0F);
        Vec3d lookVec = user.getRotationVec(1.0F);
        double reach = 16.0;
        Vec3d end = eyePos.add(lookVec.multiply(reach));

        BlockHitResult hitResult = world.raycast(new RaycastContext(
                eyePos, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                user
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();
            Direction face = hitResult.getSide();
            Direction backFace = face.getOpposite();

            Vec3d startVec = hitPos.toCenterPos().add(
                    backFace.getOffsetX() * 0.5,
                    backFace.getOffsetY() * 0.5,
                    backFace.getOffsetZ() * 0.5
            );

            Vec3d step = lookVec.normalize().multiply(0.5);
            double maxDepth = 16.0;
            int maxSteps = (int) (maxDepth / 0.5);
            boolean found = false;
            String foundTypeKey = "";
            double foundDepth = 0;

            for (int i = 1; i <= maxSteps; i++) {
                Vec3d checkPos = startVec.add(step.multiply(i));
                BlockPos checkBlockPos = BlockPos.ofFloored(checkPos);

                if (checkBlockPos.getY() < world.getBottomY() || checkBlockPos.getY() >= world.getTopY())
                    break;

                BlockState state = world.getBlockState(checkBlockPos);

                if (state.isAir()) {
                    found = true;
                    foundTypeKey = "tooltip.doctor_m.scan.type.air";
                    foundDepth = (i - 1) * 0.5;
                    break;
                }

                if (!state.getFluidState().isEmpty()) {
                    found = true;
                    foundTypeKey = "tooltip.doctor_m.scan.type.liquid";
                    foundDepth = (i - 1) * 0.5;
                    break;
                }

                if (isPassable(state)) {
                    found = true;
                    foundTypeKey = "tooltip.doctor_m.scan.type.passable";
                    foundDepth = (i - 1) * 0.5;
                    break;
                }

                if (state.isSolidBlock(world, checkBlockPos)) {
                    continue;
                }

                found = true;
                foundTypeKey = "tooltip.doctor_m.scan.type.space";
                foundDepth = (i - 1) * 0.5;
                break;
            }

            if (found) {
                user.sendMessage(
                        Text.translatable("tooltip.doctor_m.scan.space_found", foundDepth, Text.translatable(foundTypeKey))
                                .formatted(Formatting.AQUA),
                        false
                );
            }
        }
    }

    private boolean isPassable(BlockState state) {
        if (state.getBlock() instanceof net.minecraft.block.PlantBlock) return true;
        if (state.getBlock() instanceof net.minecraft.block.SnowBlock) return true;
        if (state.getBlock() instanceof net.minecraft.block.VineBlock) return true;
        if (state.getBlock() instanceof net.minecraft.block.TorchBlock) return true;
        if (state.getBlock() instanceof net.minecraft.block.CarpetBlock) return true;
        if (state.getBlock() instanceof net.minecraft.block.AbstractSignBlock) return true;
        if (state.getBlock() instanceof net.minecraft.block.DoorBlock && state.get(net.minecraft.block.DoorBlock.OPEN)) return true;
        return state.getCollisionShape(null, null).isEmpty();
    }

    // ========== 扫描区域：坐标、维度、时间 ==========
    @Inject(method = "scanRegion", at = @At("RETURN"), cancellable = false)
    private void onScanRegion(ItemStack stack, World world, PlayerEntity user, BlockPos pos,
                              CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient() || user == null) return;

        String dimension = world.getRegistryKey().getValue().toString();
        long timeOfDay = world.getTimeOfDay() % 24000;
        int hours = (int) (timeOfDay / 1000 + 6) % 24;
        int minutes = (int) ((timeOfDay % 1000) * 60 / 1000);
        String timeStr = String.format("%02d:%02d", hours, minutes);

        user.sendMessage(
                Text.translatable("tooltip.doctor_m.scan.region_info",
                                pos.getX(), pos.getY(), pos.getZ(), dimension, timeStr)
                        .formatted(Formatting.GRAY),
                false
        );
    }

    // ========== 扫描实体：类型 + 敌对/友好 ==========
    @Inject(method = "scanEntities", at = @At("RETURN"), cancellable = false)
    private void onScanEntities(ItemStack stack, World world, PlayerEntity user, Entity entity,
                                CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient() || user == null || entity == null) return;

        Text info = getEntityInfo(entity);
        if (info != null) {
            user.sendMessage(info, false);
        }
    }

    private Text getEntityInfo(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return Text.translatable("tooltip.doctor_m.scan.entity.non_living").formatted(Formatting.GRAY);
        }

        String typeKey = "tooltip.doctor_m.scan.entity.unknown";

        // 只检测三种特定生物类型
        if (living.isUndead()) {
            typeKey = "tooltip.doctor_m.scan.entity.undead";
        } else if (isArthropod(entity)) {
            typeKey = "tooltip.doctor_m.scan.entity.arthropod";
        } else if (entity.getType().isIn(EntityTypeTags.RAIDERS)) {
            typeKey = "tooltip.doctor_m.scan.entity.illager";
        }
        // 其他所有情况都返回“未知”

        return Text.translatable("tooltip.doctor_m.scan.entity.format", Text.translatable(typeKey))
                .formatted(Formatting.YELLOW);
    }

    private boolean isArthropod(Entity entity) {
        return entity instanceof SpiderEntity
                || entity instanceof CaveSpiderEntity
                || entity instanceof SilverfishEntity
                || entity instanceof EndermiteEntity
                || entity instanceof BeeEntity;
    }
}