package com.example.doctor_m.mixin.SonicMode;

import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.entity.EntityType;
import dev.amble.ait.core.item.sonic.ScanningSonicMode;
import dev.amble.ait.core.item.sonic.SonicMode;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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
            // 非生物实体 → 返回 null（不显示任何消息）
            return null;
        }

        String typeKey = null;
        boolean isBaby = living.isBaby();

        // ===== 幼年优先 =====
        if (isBaby) {
            typeKey = "tooltip.doctor_m.scan.entity.baby";
        }
        // ===== 亡灵 =====
        else if (living.isUndead()) {
            typeKey = "tooltip.doctor_m.scan.entity.undead";
        }
        // ===== 节肢 =====
        else if (isArthropod(entity)) {
            typeKey = "tooltip.doctor_m.scan.entity.arthropod";
        }
        // ===== 标签检测 =====
        else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "fish")))) {
            typeKey = "tooltip.doctor_m.scan.entity.fish";
        }
        else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "water")))) {
            typeKey = "tooltip.doctor_m.scan.entity.aquatic";
        }
        else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "raiders")))) {
            typeKey = "tooltip.doctor_m.scan.entity.illager";
        }
        else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "skeletons")))) {
            typeKey = "tooltip.doctor_m.scan.entity.skeleton";
        }
        else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "zombies")))) {
            typeKey = "tooltip.doctor_m.scan.entity.zombie";
        }
        // ===== 具体实例检测 =====
        else if (entity instanceof PigEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.pig";
        }
        else if (entity instanceof SheepEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.sheep";
        }
        else if (entity instanceof CowEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.cow";
        }
        else if (entity instanceof ChickenEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.chicken";
        }
        else if (entity instanceof RabbitEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.rabbit";
        }
        else if (entity instanceof PandaEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.panda";
        }
        else if (entity instanceof FoxEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.fox";
        }
        else if (entity instanceof CatEntity || entity instanceof OcelotEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.feline";
        }
        else if (entity instanceof WolfEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.wolf";
        }
        else if (entity instanceof HorseEntity || entity instanceof DonkeyEntity ||
                entity instanceof MuleEntity || entity instanceof SkeletonHorseEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.equine";
        }
        else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.villager";
        }
        else if (entity instanceof IronGolemEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.golem";
        }
        else if (entity instanceof SnowGolemEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.snow_golem";
        }
        else if (entity instanceof CreeperEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.creeper";
        }
        else if (entity instanceof EndermanEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.enderman";
        }
        else if (entity instanceof BlazeEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.blaze";
        }
        else if (entity instanceof MagmaCubeEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.magma_cube";
        }
        else if (entity instanceof SlimeEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.slime";
        }
        else if (entity instanceof GhastEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.ghast";
        }
        else if (entity instanceof ShulkerEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.shulker";
        }
        else if (entity instanceof EnderDragonEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.dragon";
        }
        else if (entity instanceof WitherEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.wither";
        }
        else if (entity instanceof GuardianEntity || entity instanceof ElderGuardianEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.guardian";
        }
        // ===== 大型分类兜底 =====
        else if (entity instanceof Monster) {
            typeKey = "tooltip.doctor_m.scan.entity.monster";
        }
        else if (entity instanceof PassiveEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.passive";
        }
        else if (entity instanceof ParrotEntity || entity instanceof BatEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.flying";
        }
        // ===== 自定义标签 =====
        else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("doctor_m", "103_tardis_tag")))) {
            typeKey = "tooltip.doctor_m.scan.entity.type_103";
        }
        // ===== 未匹配 → 返回 null（不显示任何消息） =====
        else {
            return null;
        }

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