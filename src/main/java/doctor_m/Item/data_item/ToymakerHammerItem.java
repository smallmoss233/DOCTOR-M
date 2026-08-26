package doctor_m.Item.data_item;

import dev.amble.ait.AITMod;
import dev.amble.ait.api.tardis.TardisComponent;
import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import dev.amble.ait.core.blocks.ExteriorBlock;
import dev.amble.ait.core.engine.DurableSubSystem;
import dev.amble.ait.core.engine.SubSystem;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.handler.LoyaltyHandler;
import dev.amble.ait.core.tardis.handler.StatsHandler;
import dev.amble.ait.core.tardis.handler.SubSystemHandler;
import dev.amble.ait.core.tardis.handler.travel.ProgressiveTravelHandler;
import dev.amble.ait.core.tardis.handler.travel.TravelHandlerBase;
import dev.amble.ait.core.tardis.manager.ServerTardisManager;
import dev.amble.ait.core.tardis.manager.TardisBuilder;
import dev.amble.ait.data.Loyalty;
import dev.amble.ait.data.properties.Value;
import dev.amble.ait.data.schema.desktop.TardisDesktopSchema;
import dev.amble.ait.data.schema.exterior.ExteriorVariantSchema;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ToymakerHammerItem extends Item {

    private static final int MIN_CHARGE_TICKS = 40;
    private static final int COPY_CHUNK_RADIUS = 20; // 320 格范围

    public ToymakerHammerItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;
        player.setCurrentHand(context.getHand());
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient()) return;
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        int chargeTicks = this.getMaxUseTime(stack) - remainingUseTicks;
        if (chargeTicks < MIN_CHARGE_TICKS) return;

        Vec3d eyePos = player.getEyePos();
        Vec3d reachPos = eyePos.add(player.getRotationVec(1.0F).multiply(5.0));
        BlockHitResult hit = world.raycast(new RaycastContext(
                eyePos, reachPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos targetPos = hit.getBlockPos();
        if (!(world.getBlockEntity(targetPos) instanceof ExteriorBlockEntity exterior)) {
            player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.not_tardis"), true);
            return;
        }

        if (exterior.tardis().isEmpty()) return;
        Tardis source = exterior.tardis().get();
        if (!(source instanceof ServerTardis sourceServer)) return;

        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d horizontalLook = new Vec3d(lookVec.x, 0, lookVec.z);
        if (horizontalLook.lengthSquared() < 0.01) horizontalLook = new Vec3d(0, 0, -1);
        horizontalLook = horizontalLook.normalize();

        BlockPos spawnPos = targetPos.add(
                (int) Math.round(horizontalLook.x * 2), 0,
                (int) Math.round(horizontalLook.z * 2)
        );
        byte sourceRotation = (byte) (int) world.getBlockState(targetPos).get(ExteriorBlock.ROTATION);

        player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.cloning_start"), true);

        boolean success = cloneTardisFull(sourceServer, player, serverWorld, spawnPos, sourceRotation);

        if (success) {
            spawnEffects(serverWorld, targetPos, spawnPos);
            player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.success",
                    source.stats().getName()), false);
        }
    }

    private boolean cloneTardisFull(ServerTardis source, ServerPlayerEntity player,
                                    ServerWorld world, BlockPos spawnPos, byte rotation) {
        try {
            UUID newUuid = UUID.randomUUID();

            TardisDesktopSchema desktopSchema = getDesktopSchema(source);
            ExteriorVariantSchema exteriorVariant = getExteriorVariant(source);

            CachedDirectedGlobalPos newPos = CachedDirectedGlobalPos.create(world, spawnPos, rotation);

            TardisBuilder builder = new TardisBuilder(newUuid)
                    .at(newPos);

            if (desktopSchema != null) builder.desktop(desktopSchema);
            if (exteriorVariant != null) builder.exterior(exteriorVariant);

            builder.<StatsHandler>with(TardisComponent.Id.STATS, stats -> copyStatsHandler(source, stats));

            builder.<LoyaltyHandler>with(TardisComponent.Id.LOYALTY, loyalty -> copyLoyaltyHandler(source, loyalty));

            ServerTardis clone = ServerTardisManager.getInstance().create(builder);
            if (clone == null) {
                player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.manager_full"), true);
                return false;
            }

            copyTravelStateSafe(source, clone);
            copySubSystems(source, clone);
            clone.world();
            copyInteriorDimension(source, clone, player);
            clone.travel().placeExterior(false);
            clone.loyalty().set(player, new Loyalty(Loyalty.Type.COMPANION));
            AITMod.LOGGER.info("[DOCTOR-M] Full clone complete: {} -> {}", source.getUuid(), clone.getUuid());
            return true;

        } catch (Exception e) {
            AITMod.LOGGER.error("[DOCTOR-M] Full clone failed", e);
            player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.error"), true);
            return false;
        }
    }

    private TardisDesktopSchema getDesktopSchema(ServerTardis source) {
        try {
            Object desktop = invokeSafe(source, "getDesktop");
            if (desktop == null) return null;
            Object schema = invokeSafe(desktop, "getSchema");
            return (TardisDesktopSchema) schema;
        } catch (Exception e) {
            AITMod.LOGGER.warn("[DOCTOR-M] Failed to get desktop schema: {}", e.getMessage());
            return null;
        }
    }

    private ExteriorVariantSchema getExteriorVariant(ServerTardis source) {
        try {
            Object exterior = invokeSafe(source, "getExterior");
            if (exterior == null) return null;
            Object variant = invokeSafe(exterior, "getVariant");
            return (ExteriorVariantSchema) variant;
        } catch (Exception e) {
            AITMod.LOGGER.warn("[DOCTOR-M] Failed to get exterior variant: {}", e.getMessage());
            return null;
        }
    }

    private void copyStatsHandler(ServerTardis source, StatsHandler target) {
        try {
            StatsHandler src = source.stats();

            String name = (String) invokeSafe(src, "getName");
            if (name != null) invokeSafe(target, "setName", name);

            String creator = (String) invokeSafe(src, "getPlayerCreatorName");
            if (creator != null) {
                invokeSafe(target, "setPlayerCreatorName", creator);
                invokeSafe(target, "markPlayerCreatorName");
            }

            copyAllValueFields(src, target);

        } catch (Exception e) {
            AITMod.LOGGER.warn("[DOCTOR-M] Stats copy warning: {}", e.getMessage());
        }
    }

    private void copyLoyaltyHandler(ServerTardis source, LoyaltyHandler target) {
        try {
            LoyaltyHandler src = source.loyalty();

            Field srcMapField = null;
            for (Field f : src.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(src);
                if (val instanceof Map<?, ?> map && !map.isEmpty()) {
                    srcMapField = f;
                    break;
                }
            }

            if (srcMapField == null) {
                AITMod.LOGGER.warn("[DOCTOR-M] Could not find loyalty map field in source");
                return;
            }

            Field dstMapField = null;
            for (Field f : target.getClass().getDeclaredFields()) {
                if (f.getType() == srcMapField.getType()) {
                    dstMapField = f;
                    break;
                }
            }
            if (dstMapField == null) {
                dstMapField = findField(target.getClass(), srcMapField.getName());
            }

            if (dstMapField != null) {
                srcMapField.setAccessible(true);
                dstMapField.setAccessible(true);

                @SuppressWarnings("unchecked")
                Map<UUID, Loyalty> srcMap = (Map<UUID, Loyalty>) srcMapField.get(src);
                @SuppressWarnings("unchecked")
                Map<UUID, Loyalty> dstMap = (Map<UUID, Loyalty>) dstMapField.get(target);

                if (srcMap != null && dstMap != null) {
                    dstMap.putAll(srcMap);
                }
            }

        } catch (Exception e) {
            AITMod.LOGGER.warn("[DOCTOR-M] Loyalty copy warning: {}", e.getMessage());
        }
    }

    private void copyTravelStateSafe(ServerTardis source, ServerTardis clone) {
        try {
            TravelHandlerBase srcBase = source.travel();
            TravelHandlerBase dstBase = clone.travel();

            copyValueField(srcBase, dstBase, "state");
            copyValueField(srcBase, dstBase, "speed");
            copyValueField(srcBase, dstBase, "maxSpeed");
            copyValueField(srcBase, dstBase, "crashing");
            copyValueField(srcBase, dstBase, "antigravs");
            copyValueField(srcBase, dstBase, "leaveBehind");
            copyValueField(srcBase, dstBase, "vGroundSearch");
            copyValueField(srcBase, dstBase, "hGroundSearch");

            CachedDirectedGlobalPos prev = srcBase.previousPosition();
            if (prev != null) {
                Field f = findField(TravelHandlerBase.class, "previousPosition");
                if (f != null) {
                    f.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Value<CachedDirectedGlobalPos> val = (Value<CachedDirectedGlobalPos>) f.get(dstBase);
                    val.set(prev);
                }
            }

            if (srcBase instanceof ProgressiveTravelHandler srcProg && dstBase instanceof ProgressiveTravelHandler dstProg) {

                Field capField = findField(ProgressiveTravelHandler.class, "missedHardCap");
                if (capField != null) {
                    capField.setAccessible(true);
                    int srcCap = capField.getInt(srcProg);

                    capField.setInt(dstProg, Math.max(srcCap, 1));
                }

                Field evtField = findField(ProgressiveTravelHandler.class, "missedEvents");
                if (evtField != null) {
                    evtField.setAccessible(true);
                    evtField.setInt(dstProg, evtField.getInt(srcProg));
                }

                copyValueField(srcProg, dstProg, "flightTicks");
                copyValueField(srcProg, dstProg, "targetTicks");
                copyValueField(srcProg, dstProg, "handbrake");
                copyValueField(srcProg, dstProg, "autopilot");
            }

        } catch (Exception e) {
            AITMod.LOGGER.warn("[DOCTOR-M] Travel state copy warning: {}", e.getMessage());
        }
    }

    private void copySubSystems(ServerTardis source, ServerTardis clone) {
        try {
            SubSystemHandler srcSub = source.subsystems();
            SubSystemHandler dstSub = clone.subsystems();

            srcSub.forEach(srcSys -> {
                try {
                    SubSystem dstSys = dstSub.get(srcSys.getId());
                    if (dstSys == null) return;

                    if (srcSys.isEnabled() != dstSys.isEnabled()) {
                        dstSys.setEnabled(srcSys.isEnabled());
                    }

                    if (srcSys instanceof DurableSubSystem srcDur && dstSys instanceof DurableSubSystem dstDur) {
                        Field durField = findField(DurableSubSystem.class, "durability");
                        if (durField != null) {
                            durField.setAccessible(true);
                            int d = durField.getInt(srcDur);
                            dstDur.setDurability(d);
                        }
                    }
                } catch (Exception e) {
                    AITMod.LOGGER.warn("[DOCTOR-M] Subsystem {} copy warning: {}", srcSys.getId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            AITMod.LOGGER.warn("[DOCTOR-M] Subsystem copy warning: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void copyValueField(Object srcObj, Object dstObj, String fieldName) {
        try {
            Field f = findField(srcObj.getClass(), fieldName);
            if (f == null) return;
            f.setAccessible(true);

            Object srcVal = f.get(srcObj);
            Object dstVal = f.get(dstObj);

            if (srcVal instanceof Value<?> s && dstVal instanceof Value<?> d) {
                Object data = s.get();
                ((Value<Object>) d).set(data);
            }
        } catch (Exception ignored) {}
    }

    private void copyAllValueFields(Object src, Object dst) {
        Class<?> clazz = src.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if (!Value.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object s = f.get(src);
                    Object d = f.get(dst);
                    if (s instanceof Value<?> sv && d instanceof Value<?> dv) {
                        ((Value<Object>) dv).set(sv.get());
                    }
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static Object invokeSafe(Object obj, String method, Object... args) {
        if (obj == null) return null;
        try {
            Class<?>[] types = new Class[args.length];
            for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
            return obj.getClass().getMethod(method, types).invoke(obj, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private void copyInteriorDimension(ServerTardis source, ServerTardis clone, ServerPlayerEntity player) {
        ServerWorld srcWorld = source.world();
        ServerWorld dstWorld = clone.world();

        int copiedBlocks = 0;
        int copiedBlockEntities = 0;
        int copiedEntities = 0;

        for (int cx = -COPY_CHUNK_RADIUS; cx <= COPY_CHUNK_RADIUS; cx++) {
            for (int cz = -COPY_CHUNK_RADIUS; cz <= COPY_CHUNK_RADIUS; cz++) {
                Chunk srcChunkRaw;
                try {
                    srcChunkRaw = srcWorld.getChunk(cx, cz);
                } catch (Exception e) {
                    continue;
                }

                if (!(srcChunkRaw instanceof WorldChunk srcChunk)) continue;

                boolean hasBlocks = false;
                ChunkSection[] srcSections = srcChunk.getSectionArray();
                for (ChunkSection section : srcSections) {
                    if (section != null && !section.isEmpty()) {
                        hasBlocks = true;
                        break;
                    }
                }
                if (!hasBlocks) continue;

                Chunk dstChunkRaw = dstWorld.getChunk(cx, cz);
                if (!(dstChunkRaw instanceof WorldChunk)) continue;

                int baseX = cx << 4;
                int baseZ = cz << 4;
                int bottomY = srcChunk.getBottomY();

                for (int sy = 0; sy < srcSections.length; sy++) {
                    ChunkSection section = srcSections[sy];
                    if (section == null || section.isEmpty()) continue;

                    int sectionBaseY = bottomY + (sy << 4);

                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 16; y++) {
                                BlockState state = section.getBlockState(x, y, z);
                                if (state.isAir()) continue;
                                if (state.getBlock() instanceof ExteriorBlock) continue;

                                BlockPos pos = new BlockPos(baseX + x, sectionBaseY + y, baseZ + z);
                                dstWorld.setBlockState(pos, state, 2 | 16);

                                BlockEntity srcBe = srcWorld.getBlockEntity(pos);
                                if (srcBe != null) {
                                    NbtCompound nbt = srcBe.createNbtWithIdentifyingData();
                                    replaceUuidDeep(nbt, source.getUuid(), clone.getUuid());

                                    BlockEntity dstBe = dstWorld.getBlockEntity(pos);
                                    if (dstBe != null) {
                                        dstBe.readNbt(nbt);
                                        dstBe.markDirty();
                                    }
                                    copiedBlockEntities++;
                                }
                                copiedBlocks++;
                            }
                        }
                    }
                }
            }
        }

        Box searchBox = new Box(
                -384, dstWorld.getBottomY(), -384,
                384, dstWorld.getTopY(), 384
        );

        for (Entity entity : srcWorld.getEntitiesByClass(Entity.class, searchBox,
                e -> !(e instanceof PlayerEntity))) {
            try {
                NbtCompound nbt = new NbtCompound();
                if (!entity.saveNbt(nbt)) continue;

                replaceUuidDeep(nbt, source.getUuid(), clone.getUuid());
                nbt.remove("UUID");
                nbt.remove("Pos");

                Entity newEntity = entity.getType().create(dstWorld);
                if (newEntity == null) continue;

                newEntity.readNbt(nbt);
                newEntity.setUuid(UUID.randomUUID());
                newEntity.refreshPositionAndAngles(
                        entity.getX(), entity.getY(), entity.getZ(),
                        entity.getYaw(), entity.getPitch()
                );
                dstWorld.spawnEntity(newEntity);
                copiedEntities++;
            } catch (Exception e) {
                AITMod.LOGGER.warn("[DOCTOR-M] Failed to copy entity {}: {}", entity.getType(), e.getMessage());
            }
        }

        AITMod.LOGGER.info("[DOCTOR-M] Interior copied: {} blocks, {} BEs, {} entities",
                copiedBlocks, copiedBlockEntities, copiedEntities);
    }

    private static void replaceUuidDeep(NbtCompound nbt, UUID oldUuid, UUID newUuid) {
        if (nbt == null) return;
        String oldStr = oldUuid.toString();

        for (String key : new java.util.HashSet<>(nbt.getKeys())) {
            NbtElement element = nbt.get(key);
            if (element == null) continue;

            if (element instanceof NbtCompound sub) {
                if (sub.contains("MOST", NbtElement.LONG_TYPE) && sub.contains("LEAST", NbtElement.LONG_TYPE)) {
                    UUID uuid = new UUID(sub.getLong("MOST"), sub.getLong("LEAST"));
                    if (uuid.equals(oldUuid)) {
                        sub.putLong("MOST", newUuid.getMostSignificantBits());
                        sub.putLong("LEAST", newUuid.getLeastSignificantBits());
                        continue;
                    }
                }
                replaceUuidDeep(sub, oldUuid, newUuid);
            } else if (element instanceof NbtList list) {
                for (int i = 0; i < list.size(); i++) {
                    NbtElement item = list.get(i);
                    if (item instanceof NbtCompound sub) {
                        replaceUuidDeep(sub, oldUuid, newUuid);
                    } else if (item instanceof NbtString str && str.asString().equals(oldStr)) {
                        list.set(i, NbtString.of(newUuid.toString()));
                    }
                }
            } else if (element instanceof NbtString str && str.asString().equals(oldStr)) {
                nbt.putString(key, newUuid.toString());
            } else if (element instanceof NbtIntArray arr && arr.size() == 4) {
                try {
                    UUID arrUuid = NbtHelper.toUuid(arr);
                    if (arrUuid.equals(oldUuid)) {
                        nbt.put(key, NbtHelper.fromUuid(newUuid));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void spawnEffects(ServerWorld world, BlockPos oldPos, BlockPos newPos) {
        double oldX = oldPos.getX() + 0.5;
        double oldY = oldPos.getY() + 1.0;
        double oldZ = oldPos.getZ() + 0.5;
        double newX = newPos.getX() + 0.5;
        double newY = newPos.getY() + 1.0;
        double newZ = newPos.getZ() + 0.5;

        world.playSound(null, oldX, oldY, oldZ,
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.5F, 0.9F);
        world.playSound(null, newX, newY, newZ,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 2.0F, 0.7F);

        world.spawnParticles(ParticleTypes.EXPLOSION, oldX, oldY, oldZ, 3, 0.2, 0.5, 0.2, 0.1);
        world.spawnParticles(ParticleTypes.FLASH, oldX, oldY, oldZ, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.CLOUD, oldX, oldY, oldZ, 25, 0.5, 0.8, 0.5, 0.05);
        world.spawnParticles(ParticleTypes.END_ROD, oldX, oldY, oldZ, 50, 0.8, 1.0, 0.8, 0.1);
        world.spawnParticles(ParticleTypes.CRIT, oldX, oldY, oldZ, 30, 0.6, 0.6, 0.6, 0.2);

        world.spawnParticles(ParticleTypes.PORTAL, newX, newY, newZ, 120, 1.0, 1.5, 1.0, 0.3);
        world.spawnParticles(ParticleTypes.ENCHANT, newX, newY, newZ, 80, 1.2, 1.2, 1.2, 0.2);
        world.spawnParticles(ParticleTypes.END_ROD, newX, newY, newZ, 60, 0.5, 1.0, 0.5, 0.05);
        world.spawnParticles(ParticleTypes.WITCH, newX, newY, newZ, 40, 0.8, 0.8, 0.8, 0.1);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Text longDescription = Text.translatable("message.doctor_m.toymaker_hammer.tip");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.toymaker_hammer.detail")
        );
    }
}