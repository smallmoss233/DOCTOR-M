package doctor_m.Item.data_itme;

import doctor_m.Item.KeytoTime;
import doctor_m.util.tooltip.ShiftTooltipInvoker;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TracerItem extends Item {

    private static final double SCAN_RANGE = 100;
    private static final int CONTAINER_SCAN_RANGE = 100;
    private static final double SCAN_RANGE_SQ = SCAN_RANGE * SCAN_RANGE;

    private int tickCooldown = 0;

    public TracerItem(Settings settings) {
        super(settings.maxCount(1).fireproof());
    }

    // ========== 手持自动扫描（掉落物 + 展示框 + 生物携带） ==========
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!world.isClient || !(entity instanceof PlayerEntity player)) return;

        boolean held = player.getMainHandStack() == stack || player.getOffHandStack() == stack;
        if (!held) return;

        if (tickCooldown-- > 0) return;

        Vec3d eye = player.getEyePos();
        double nearestSq = SCAN_RANGE_SQ + 1;
        Box box = new Box(eye, eye).expand(SCAN_RANGE);

        // 扫描掉落物
        for (ItemEntity itemEntity : world.getEntitiesByClass(
                ItemEntity.class, box,
                e -> e.getStack().getItem() instanceof KeytoTime)) {
            double d = itemEntity.squaredDistanceTo(eye);
            if (d < nearestSq) nearestSq = d;
        }

        // 扫描物品展示框（含荧光展示框）
        for (ItemFrameEntity frame : world.getEntitiesByClass(
                ItemFrameEntity.class, box,
                e -> e.getHeldItemStack().getItem() instanceof KeytoTime)) {
            double d = frame.squaredDistanceTo(eye);
            if (d < nearestSq) nearestSq = d;
        }

        // 扫描生物携带（主副手 + 物品栏）
        for (LivingEntity living : world.getEntitiesByClass(
                LivingEntity.class, box,
                e -> {
                    if (e instanceof Inventory inv) {
                        for (int i = 0; i < inv.size(); i++) {
                            if (inv.getStack(i).getItem() instanceof KeytoTime) return true;
                        }
                    }
                    return e.getMainHandStack().getItem() instanceof KeytoTime
                            || e.getOffHandStack().getItem() instanceof KeytoTime;
                })) {
            double d = living.squaredDistanceTo(eye);
            if (d < nearestSq) nearestSq = d;
        }

        if (nearestSq > SCAN_RANGE_SQ) {
            tickCooldown = 30;
            return;
        }

        double dist = Math.sqrt(nearestSq);
        double ratio = dist / SCAN_RANGE;

        tickCooldown = (int) (3 + ratio * 20);
        float volume = 1.0f - (float) (ratio * 0.7f);
        float pitch = 1.0f + (float) ((1.0 - ratio) * 0.6f);

        world.playSound(
                player, player.getBlockPos(),
                SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
                SoundCategory.PLAYERS,
                volume, pitch
        );

        if (dist < 6.0) {
            world.addParticle(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.2, player.getZ(),
                    0, 0.02, 0);
        }
    }

    // ========== 右键手动扫描（容器 + 掉落物 + 展示框 + 生物携带） ==========
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        Vec3d playerPos = user.getPos();
        double nearestSq = Double.MAX_VALUE;
        boolean found = false;
        boolean inContainer = false;
        boolean inFrame = false;
        boolean inEntity = false;

        Box entityBox = new Box(playerPos, playerPos).expand(SCAN_RANGE);

        // 1. 扫描掉落物
        for (ItemEntity itemEntity : world.getEntitiesByClass(
                ItemEntity.class, entityBox,
                e -> e.getStack().getItem() instanceof KeytoTime)) {
            double d = itemEntity.squaredDistanceTo(playerPos);
            if (d < nearestSq) {
                nearestSq = d;
                found = true;
                inContainer = false;
                inFrame = false;
                inEntity = false;
            }
        }

        // 2. 扫描物品展示框（含荧光展示框）
        for (ItemFrameEntity frame : world.getEntitiesByClass(
                ItemFrameEntity.class, entityBox,
                e -> e.getHeldItemStack().getItem() instanceof KeytoTime)) {
            double d = frame.squaredDistanceTo(playerPos);
            if (d < nearestSq) {
                nearestSq = d;
                found = true;
                inContainer = false;
                inFrame = true;
                inEntity = false;
            }
        }

        // 3. 扫描生物携带（主副手 + 物品栏）
        for (LivingEntity living : world.getEntitiesByClass(
                LivingEntity.class, entityBox,
                e -> {
                    if (e instanceof Inventory inv) {
                        for (int i = 0; i < inv.size(); i++) {
                            if (inv.getStack(i).getItem() instanceof KeytoTime) return true;
                        }
                    }
                    return e.getMainHandStack().getItem() instanceof KeytoTime
                            || e.getOffHandStack().getItem() instanceof KeytoTime;
                })) {
            double d = living.squaredDistanceTo(playerPos);
            if (d < nearestSq) {
                nearestSq = d;
                found = true;
                inContainer = false;
                inFrame = false;
                inEntity = true;
            }
        }

        // 4. 扫描容器
        BlockPos center = user.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(
                center.add(-CONTAINER_SCAN_RANGE, -CONTAINER_SCAN_RANGE, -CONTAINER_SCAN_RANGE),
                center.add(CONTAINER_SCAN_RANGE, CONTAINER_SCAN_RANGE, CONTAINER_SCAN_RANGE))) {

            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof Inventory inv)) continue;

            boolean hasFragment = false;
            for (int i = 0; i < inv.size(); i++) {
                if (inv.getStack(i).getItem() instanceof KeytoTime) {
                    hasFragment = true;
                    break;
                }
            }
            if (!hasFragment) continue;

            double d = Vec3d.ofCenter(pos).squaredDistanceTo(playerPos);
            if (d < nearestSq) {
                nearestSq = d;
                found = true;
                inContainer = true;
                inFrame = false;
                inEntity = false;
            }
        }

        if (found) {
            int distance = (int) Math.sqrt(nearestSq);
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                    SoundCategory.PLAYERS, 1.0f, 2.0f);

            String key;
            if (inContainer) {
                key = "message.doctor_m.tracer.scan_found_container";
            } else if (inFrame) {
                key = "message.doctor_m.tracer.scan_found_frame";
            } else if (inEntity) {
                key = "message.doctor_m.tracer.scan_found_entity";
            } else {
                key = "message.doctor_m.tracer.scan_found_ground";
            }
            user.sendMessage(Text.translatable(key, distance), true);
        } else {
            user.sendMessage(Text.translatable("message.doctor_m.tracer.scan_none"), true);
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("message.doctor_m.tracer.tooltip"));
        ShiftTooltipInvoker.addShiftTooltip(tooltip,
                Text.translatable("message.doctor_m.tracer.detail")
        );
    }
}