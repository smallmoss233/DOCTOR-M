package doctor_m.mixin.sonic_mode;

import dev.amble.ait.core.item.sonic.ScanningSonicMode;
import dev.amble.ait.core.item.sonic.SonicMode;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScanningSonicMode.class)
public abstract class ScanningSonicModeMixin extends SonicMode {

    protected ScanningSonicModeMixin(int index) {
        super(index);
    }

    // ==================== 扫描方块：墙后空间探测 ====================
    @Unique
    private Text doctor_m$blockAppend = null;

    @Inject(method = "scanBlocks", at = @At("HEAD"))
    private void doctor_m$onScanBlocksHead(ItemStack stack, World world, PlayerEntity user, BlockPos pos,
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
                this.doctor_m$blockAppend = Text.translatable(
                        "tooltip.doctor_m.scan.space_found",
                        foundDepth,
                        Text.translatable(foundTypeKey)
                ).formatted(Formatting.AQUA);
            }
        }
    }

    @Redirect(
            method = "scanBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V"
            )
    )
    private void doctor_m$mergeBlockMessage(PlayerEntity player, Text message, boolean overlay) {
        if (overlay && this.doctor_m$blockAppend != null) {
            Text merged = Text.literal("")
                    .append(message)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(this.doctor_m$blockAppend);
            player.sendMessage(merged, true);
            this.doctor_m$blockAppend = null;
        } else {
            player.sendMessage(message, overlay);
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

    // ==================== 扫描区域：坐标、维度、时间 ====================
    @Unique
    private static Text doctor_m$regionAppend = null;

    @Inject(method = "scanRegion", at = @At("HEAD"))
    private void doctor_m$onScanRegionHead(ItemStack stack, World world, PlayerEntity user, BlockPos pos,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient() || user == null) {
            doctor_m$regionAppend = null;
            return;
        }

        String dimension = world.getRegistryKey().getValue().toString();
        long timeOfDay = world.getTimeOfDay() % 24000;
        int hours = (int) (timeOfDay / 1000 + 6) % 24;
        int minutes = (int) ((timeOfDay % 1000) * 60 / 1000);
        String timeStr = String.format("%02d:%02d", hours, minutes);

        doctor_m$regionAppend = Text.translatable(
                "tooltip.doctor_m.scan.region_info",
                pos.getX(), pos.getY(), pos.getZ(), dimension, timeStr
        ).formatted(Formatting.GRAY);
    }

    @Redirect(
            method = "sendRiftInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V"
            )
    )
    private static void doctor_m$mergeRiftMessage(PlayerEntity player, Text message, boolean overlay) {
        if (overlay && doctor_m$regionAppend != null) {
            Text merged = Text.literal("")
                    .append(message)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(doctor_m$regionAppend);
            player.sendMessage(merged, true);
        } else {
            player.sendMessage(message, overlay);
        }
    }

    @Redirect(
            method = "sendTardisInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V",
                    ordinal = 0
            )
    )
    private static void doctor_m$mergeTardisMessage1(PlayerEntity player, Text message, boolean overlay) {
        if (overlay && doctor_m$regionAppend != null) {
            Text merged = Text.literal("")
                    .append(message)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(doctor_m$regionAppend);
            player.sendMessage(merged, true);
        } else {
            player.sendMessage(message, overlay);
        }
    }

    @Redirect(
            method = "sendTardisInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V",
                    ordinal = 1
            )
    )
    private static void doctor_m$mergeTardisMessage2(PlayerEntity player, Text message, boolean overlay) {
        if (overlay && doctor_m$regionAppend != null) {
            Text merged = Text.literal("")
                    .append(message)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(doctor_m$regionAppend);
            player.sendMessage(merged, true);
            doctor_m$regionAppend = null;
        } else {
            player.sendMessage(message, overlay);
        }
    }

    // ==================== 扫描实体：类型 + 敌对/友好 ====================
    @Unique
    private Text doctor_m$entityAppend = null;

    @Inject(method = "scanEntities", at = @At("HEAD"))
    private void doctor_m$onScanEntitiesHead(ItemStack stack, World world, PlayerEntity user, Entity entity,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient() || user == null || entity == null) return;
        this.doctor_m$entityAppend = getEntityInfo(entity);
    }

    @Redirect(
            method = "scanEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;sendMessage(Lnet/minecraft/text/Text;Z)V"
            )
    )
    private void doctor_m$mergeEntityMessage(PlayerEntity player, Text message, boolean overlay) {
        if (overlay && this.doctor_m$entityAppend != null) {
            Text merged = Text.literal("")
                    .append(message)
                    .append(Text.literal(" | ").formatted(Formatting.GRAY))
                    .append(this.doctor_m$entityAppend);
            player.sendMessage(merged, true);
            this.doctor_m$entityAppend = null;
        } else {
            player.sendMessage(message, overlay);
        }
    }

    private Text getEntityInfo(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }

        String typeKey = null;
        boolean isBaby = living.isBaby();

        if (isBaby) {
            typeKey = "tooltip.doctor_m.scan.entity.baby";
        } else if (living.isUndead()) {
            typeKey = "tooltip.doctor_m.scan.entity.undead";
        } else if (isArthropod(entity)) {
            typeKey = "tooltip.doctor_m.scan.entity.arthropod";
        } else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "fish")))) {
            typeKey = "tooltip.doctor_m.scan.entity.fish";
        } else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "water")))) {
            typeKey = "tooltip.doctor_m.scan.entity.aquatic";
        } else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "raiders")))) {
            typeKey = "tooltip.doctor_m.scan.entity.illager";
        } else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "skeletons")))) {
            typeKey = "tooltip.doctor_m.scan.entity.skeleton";
        } else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("minecraft", "zombies")))) {
            typeKey = "tooltip.doctor_m.scan.entity.zombie";
        } else if (entity instanceof PigEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.pig";
        } else if (entity instanceof SheepEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.sheep";
        } else if (entity instanceof CowEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.cow";
        } else if (entity instanceof ChickenEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.chicken";
        } else if (entity instanceof RabbitEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.rabbit";
        } else if (entity instanceof PandaEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.panda";
        } else if (entity instanceof FoxEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.fox";
        } else if (entity instanceof CatEntity || entity instanceof OcelotEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.feline";
        } else if (entity instanceof WolfEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.wolf";
        } else if (entity instanceof HorseEntity || entity instanceof DonkeyEntity ||
                entity instanceof MuleEntity || entity instanceof SkeletonHorseEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.equine";
        } else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.villager";
        } else if (entity instanceof IronGolemEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.golem";
        } else if (entity instanceof SnowGolemEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.snow_golem";
        } else if (entity instanceof CreeperEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.creeper";
        } else if (entity instanceof EndermanEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.enderman";
        } else if (entity instanceof BlazeEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.blaze";
        } else if (entity instanceof MagmaCubeEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.magma_cube";
        } else if (entity instanceof SlimeEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.slime";
        } else if (entity instanceof GhastEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.ghast";
        } else if (entity instanceof ShulkerEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.shulker";
        } else if (entity instanceof EnderDragonEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.dragon";
        } else if (entity instanceof WitherEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.wither";
        } else if (entity instanceof GuardianEntity || entity instanceof ElderGuardianEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.guardian";
        } else if (entity instanceof Monster) {
            typeKey = "tooltip.doctor_m.scan.entity.monster";
        } else if (entity instanceof PassiveEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.passive";
        } else if (entity instanceof ParrotEntity || entity instanceof BatEntity) {
            typeKey = "tooltip.doctor_m.scan.entity.flying";
        } else if (entity.getType().isIn(TagKey.of(Registries.ENTITY_TYPE.getKey(), new Identifier("doctor_m", "103_tardis_tag")))) {
            typeKey = "tooltip.doctor_m.scan.entity.type_103";
        } else {
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