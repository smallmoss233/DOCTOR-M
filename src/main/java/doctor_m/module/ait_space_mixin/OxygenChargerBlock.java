package doctor_m.module.ait_space_mixin;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
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
        // 设置默认朝向为北
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

    // 注册 facing 属性
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    // 根据玩家放置方向设置方块朝向（正面朝向玩家）
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;

        ItemStack held = player.getStackInHand(hand);

        if (held.getItem() instanceof OxygenTankItem) {
            if (OxygenTankItem.getOxygen(held) < OxygenTankItem.MAX_OXYGEN) {
                OxygenTankItem.setOxygen(held, OxygenTankItem.MAX_OXYGEN);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_fill"), true);
                player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
            } else {
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_full"), true);
            }
            return ActionResult.SUCCESS;
        }

        if (held.getItem() instanceof SpacesuitItem && ((ArmorItem) held.getItem()).getType() == ArmorItem.Type.CHESTPLATE) {
            double current = SpaceOxygenManager.getOxygen(held);
            if (current < SpaceOxygenManager.MAX_OXYGEN) {
                SpaceOxygenManager.setOxygen(held, SpaceOxygenManager.MAX_OXYGEN);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_fill"), true);
                player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
            } else {
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_full"), true);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}