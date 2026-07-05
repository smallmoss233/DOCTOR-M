package doctor_m.module.ait_space_mixin;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OxygenChargerBlock extends BlockWithEntity {

    public OxygenChargerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenChargerBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // 客户端不做实际处理
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // 获取方块实体
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof OxygenChargerBlockEntity charger)) {
            return ActionResult.PASS;
        }

        // 检查冷却
        long currentTime = world.getTime();
        if (currentTime < charger.getCooldownEndTick()) {
            long remainingSeconds = (charger.getCooldownEndTick() - currentTime) / 20;
            player.sendMessage(
                    Text.translatable("message.doctor_m.oxygen_charger.cooldown", remainingSeconds),
                    true
            );
            // 返回 SUCCESS 以阻止物品自身使用
            return ActionResult.SUCCESS;
        }

        // 处理手持物品
        ItemStack held = player.getStackInHand(hand);
        boolean charged = false;

        // 尝试充氧气瓶
        if (held.getItem() instanceof OxygenTankItem) {
            if (OxygenTankItem.getOxygen(held) < OxygenTankItem.MAX_OXYGEN) {
                OxygenTankItem.setOxygen(held, OxygenTankItem.MAX_OXYGEN);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_fill"), true);
                charged = true;
            } else {
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_full"), true);
                // 即使未充氧，也返回 SUCCESS 阻止物品使用
                return ActionResult.SUCCESS;
            }
        }
        // 尝试充航天服（胸甲）
        else if (held.getItem() instanceof SpacesuitItem && ((ArmorItem) held.getItem()).getType() == ArmorItem.Type.CHESTPLATE) {
            double current = SpaceOxygenManager.getOxygen(held);
            if (current < SpaceOxygenManager.MAX_OXYGEN) {
                SpaceOxygenManager.setOxygen(held, SpaceOxygenManager.MAX_OXYGEN);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_fill"), true);
                charged = true;
            } else {
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_full"), true);
                return ActionResult.SUCCESS;
            }
        } else {
            // 手持物品不是可充氧物品，返回 PASS 让物品自己处理（但会触发物品使用）
            // 如果你希望完全阻止物品使用，可以返回 SUCCESS 并提示无效物品
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.invalid_item"), true);
            return ActionResult.SUCCESS;
        }

        // 如果成功充氧，设置冷却（32秒 = 640 ticks）
        if (charged) {
            charger.setCooldownEndTick(world.getTime() + 32 * 20);
            charger.markDirty();
            player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
        }

        // 始终返回 SUCCESS，阻止物品自身使用
        return ActionResult.SUCCESS;
    }
}