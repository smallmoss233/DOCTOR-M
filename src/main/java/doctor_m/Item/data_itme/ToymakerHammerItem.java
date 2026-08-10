package doctor_m.Item.data_itme;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.amble.ait.AITMod;
import dev.amble.ait.api.tardis.TardisComponent;
import dev.amble.ait.core.blockentities.ExteriorBlockEntity;
import dev.amble.ait.core.blocks.ExteriorBlock;
import dev.amble.ait.core.tardis.ServerTardis;
import dev.amble.ait.core.tardis.Tardis;
import dev.amble.ait.core.tardis.manager.ServerTardisManager;
import dev.amble.ait.core.tardis.manager.TardisBuilder;
import dev.amble.ait.data.Loyalty;
import dev.amble.ait.data.schema.desktop.TardisDesktopSchema;
import dev.amble.ait.data.schema.exterior.ExteriorVariantSchema;
import dev.amble.ait.registry.impl.DesktopRegistry;
import dev.amble.ait.registry.impl.exterior.ExteriorVariantRegistry;
import dev.amble.lib.data.CachedDirectedGlobalPos;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

import java.util.List;
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

        if (chargeTicks < MIN_CHARGE_TICKS) {
            // 蓄力时间不足，静默取消
            return;
        }

        Vec3d eyePos = player.getEyePos();
        Vec3d reachPos = eyePos.add(player.getRotationVec(1.0F).multiply(5.0));
        BlockHitResult hit = world.raycast(new RaycastContext(
                eyePos, reachPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            // 未命中方块，静默取消
            return;
        }

        BlockPos targetPos = hit.getBlockPos();

        if (!(world.getBlockEntity(targetPos) instanceof ExteriorBlockEntity exterior)) {
            player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.not_tardis"), true);
            return;
        }

        if (exterior.tardis().isEmpty()) {
            // 外壳未绑定，静默取消
            return;
        }

        Tardis source = exterior.tardis().get();
        if (!(source instanceof ServerTardis sourceServer)) {
            // 非服务端 Tardis，静默取消
            return;
        }

        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d horizontalLook = new Vec3d(lookVec.x, 0, lookVec.z);
        if (horizontalLook.lengthSquared() < 0.01) {
            horizontalLook = new Vec3d(0, 0, -1);
        }
        horizontalLook = horizontalLook.normalize();

        BlockPos spawnPos = targetPos.add(
                (int) Math.round(horizontalLook.x * 2),
                0,
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

    /**
     * 核心克隆逻辑：利用AIT原生Gson序列化实现完整深拷贝
     */
    private boolean cloneTardisFull(ServerTardis source, ServerPlayerEntity player,
                                    ServerWorld world, BlockPos spawnPos, byte rotation) {
        try {
            UUID newUuid = UUID.randomUUID();

            // ===== 步骤1：使用AIT的File Gson将源TARDIS完整序列化为JSON =====
            Gson gson = ServerTardisManager.getInstance().getFileGson();
            JsonObject json = JsonParser.parseString(gson.toJson(source)).getAsJsonObject();

            // ===== 步骤2：在JSON层面替换UUID =====
            json.addProperty("uuid", newUuid.toString());

            // ===== 步骤3：反序列化为新的ServerTardis对象 =====
            ServerTardis clone = gson.fromJson(json, ServerTardis.class);

            // ===== 步骤4：初始化 =====
            Tardis.init(clone, TardisComponent.InitContext.deserialize());

            // ===== 步骤5：强制设置新的外部位置 =====
            CachedDirectedGlobalPos newPos = CachedDirectedGlobalPos.create(world, spawnPos, rotation);
            clone.travel().forcePosition(newPos);
            clone.travel().forceDestination(newPos);

            // ===== 步骤6：触发内部世界的懒加载/创建 =====
            clone.world();

            // ===== 步骤7：复制内部空间 =====
            copyInteriorDimension(source, clone, player);

            // ===== 步骤8：注册到ServerTardisManager =====
            TardisBuilder fakeBuilder = new TardisBuilder(newUuid) {
                @Override
                public ServerTardis build() {
                    return clone;
                }
            };
            ServerTardis registered = ServerTardisManager.getInstance().create(fakeBuilder);
            if (registered == null) {
                player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.manager_full"), true);
                return false;
            }

            // ===== 步骤9：在新位置放置外部方块 =====
            clone.travel().placeExterior(false);

            // ===== 步骤10：将使用者设为Companion =====
            clone.loyalty().set(player, new Loyalty(Loyalty.Type.COMPANION));

            AITMod.LOGGER.info("[DOCTOR-M] Full clone complete: {} -> {}", source.getUuid(), clone.getUuid());
            return true;

        } catch (Exception e) {
            AITMod.LOGGER.error("[DOCTOR-M] Full clone failed", e);
            player.sendMessage(Text.translatable("tooltip.doctorm.toymaker_hammer.error"), true);
            return false;
        }
    }

    private void copyInteriorDimension(ServerTardis source, ServerTardis clone, ServerPlayerEntity player) {
        ServerWorld srcWorld = source.world();
        ServerWorld dstWorld = clone.world();

        int copiedBlocks = 0;
        int copiedBlockEntities = 0;
        int copiedEntities = 0;

        for (int cx = -COPY_CHUNK_RADIUS; cx <= COPY_CHUNK_RADIUS; cx++) {
            for (int cz = -COPY_CHUNK_RADIUS; cz <= COPY_CHUNK_RADIUS; cz++) {
                // 关键修复：getChunk 强制加载，而不是 getChunkManager().getChunk(..., false)
                Chunk srcChunkRaw;
                try {
                    srcChunkRaw = srcWorld.getChunk(cx, cz);
                } catch (Exception e) {
                    continue;
                }

                if (!(srcChunkRaw instanceof WorldChunk srcChunk)) continue;

                // 跳过全空 chunk
                boolean hasBlocks = false;
                ChunkSection[] srcSections = srcChunk.getSectionArray();
                for (ChunkSection section : srcSections) {
                    if (section != null && !section.isEmpty()) {
                        hasBlocks = true;
                        break;
                    }
                }
                if (!hasBlocks) continue;

                // 确保目标 chunk 存在
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

        // 实体搜索范围同步扩大到 384×384
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

    private TardisDesktopSchema pickRandomDesktop() {
        List<TardisDesktopSchema> list = DesktopRegistry.getInstance().toList();
        return list.get(AITMod.RANDOM.nextInt(list.size()));
    }

    private ExteriorVariantSchema pickRandomExterior() {
        List<ExteriorVariantSchema> list = ExteriorVariantRegistry.getInstance().toList();
        return list.get(AITMod.RANDOM.nextInt(list.size()));
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