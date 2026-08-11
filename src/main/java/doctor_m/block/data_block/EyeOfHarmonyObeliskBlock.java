package doctor_m.block.data_block;

import dev.amble.ait.core.engine.link.tracker.FluidNetwork;
import doctor_m.block.ModBlocks;
import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import dev.amble.ait.core.engine.link.IFluidLink;
import dev.amble.ait.core.engine.link.IFluidSource;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

public class EyeOfHarmonyObeliskBlock extends BlockWithEntity implements IFluidLink {

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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : (w, pos, s, be) -> {
            if (be instanceof EyeOfHarmonyObeliskBlockEntity obelisk) {
                obelisk.tick();
            }
        };
    }

    // ========== 合并后的 onPlaced：结构放置 + 网络重建 ==========
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient()) return;

        BlockPos targetPos = pos.up();   // 本体最终位置
        BlockPos up = pos.up(2);         // 上方辅助

        if (!world.getBlockState(targetPos).isReplaceable() || !world.getBlockState(up).isReplaceable()) {
            world.removeBlock(pos, false);
            Block.dropStack(world, pos, new ItemStack(this));
            if (placer instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("obelisk.no_space").formatted(Formatting.RED), true);
            }
            return;
        }

        world.setBlockState(pos, ModBlocks.EYE_OF_HARMONY_PART.getDefaultState());
        world.setBlockState(targetPos, state);
        world.setBlockState(up, ModBlocks.EYE_OF_HARMONY_PART.getDefaultState());

        // 结构放完后，触发周围线缆网络重建
        FluidNetwork.rebuildAround((ServerWorld) world, targetPos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (moved) {
            super.onStateReplaced(state, world, pos, newState, moved);
            return;
        }

        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient()) {
                FluidNetwork.rebuildAround((ServerWorld) world, pos);
            }

            BlockPos up = pos.up();
            BlockPos down = pos.down();
            if (world.getBlockState(up).isOf(ModBlocks.EYE_OF_HARMONY_PART)) {
                world.removeBlock(up, false);
            }
            if (world.getBlockState(down).isOf(ModBlocks.EYE_OF_HARMONY_PART)) {
                world.removeBlock(down, false);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (world.isClient()) return;

        if (sourceBlock instanceof IFluidLink) {
            FluidNetwork.rebuildAround((ServerWorld) world, pos);
        }
    }

    // ========== IFluidLink 实现 ==========
    @Override
    public IFluidSource source(boolean search) {
        return null;
    }

    @Override
    public void setSource(IFluidSource source) {
    }

    @Override
    public IFluidLink last() {
        return null;
    }

    @Override
    public void setLast(IFluidLink last) {
    }
}