package doctor_m.block.data_block;

import doctor_m.block.ModBlocks;
import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class EyeOfHarmonyObeliskBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public static Consumer<EyeOfHarmonyObeliskBlockEntity> OPEN_SCREEN_CALLBACK = null;

    public EyeOfHarmonyObeliskBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EyeOfHarmonyObeliskBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // ← 放置时根据玩家水平朝向设置（面向玩家）
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient && OPEN_SCREEN_CALLBACK != null) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof EyeOfHarmonyObeliskBlockEntity obelisk) {
                OPEN_SCREEN_CALLBACK.accept(obelisk);
            }
        }
        return ActionResult.SUCCESS;
    }
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient()) return;

        BlockPos targetPos = pos.up();   // 本体最终位置
        BlockPos up = pos.up(2);         // 上方辅助

        // 只检查本体位置和上方辅助位置是否可替换
        if (!world.getBlockState(targetPos).isReplaceable() || !world.getBlockState(up).isReplaceable()) {
            // 空间不足，移除本体并提示
            world.removeBlock(pos, false);
            Block.dropStack(world, pos, new ItemStack(this));
            if (placer instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("obelisk.no_space").formatted(Formatting.RED), true);
            }
            return;
        }

        // 将 pos 变成下方辅助方块
        world.setBlockState(pos, ModBlocks.EYE_OF_HARMONY_PART.getDefaultState());
        // 在 targetPos 放置本体
        world.setBlockState(targetPos, state);
        // 在 up 放置上方辅助
        world.setBlockState(up, ModBlocks.EYE_OF_HARMONY_PART.getDefaultState());
    }
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (moved) return;
        if (!state.isOf(newState.getBlock())) {
            BlockPos up = pos.up();
            BlockPos down = pos.down();
            // 使用 isOf 替代 instanceof
            if (world.getBlockState(up).isOf(ModBlocks.EYE_OF_HARMONY_PART)) {
                world.removeBlock(up, false);
            }
            if (world.getBlockState(down).isOf(ModBlocks.EYE_OF_HARMONY_PART)) {
                world.removeBlock(down, false);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}