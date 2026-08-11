package doctor_m.block.data_block;

import doctor_m.block.ModBlocks;
import doctor_m.block.entities.EyeOfHarmonyPartBlockEntity;
import dev.amble.ait.core.engine.link.IFluidLink;
import dev.amble.ait.core.engine.link.IFluidSource;
import dev.amble.ait.core.engine.link.tracker.FluidNetwork;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class EyeOfHarmonyPartBlock extends BlockWithEntity implements IFluidLink {

    public EyeOfHarmonyPartBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EyeOfHarmonyPartBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Block.createCuboidShape(0, 0, 0, 16, 16, 16);
    }

    @Override
    public String getTranslationKey() {
        return ModBlocks.EYE_OF_HARMONY_OBELISK.getTranslationKey();
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient()) {
            FluidNetwork.rebuildAround((ServerWorld) world, pos);
        }
    }

    @SuppressWarnings("deprecation")
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

            BlockPos mainPos = findMainBlock(world, pos);
            if (mainPos != null) {
                destroyStructure(world, mainPos);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (world.isClient()) return;

        if (sourceBlock instanceof IFluidLink) {
            FluidNetwork.rebuildAround((ServerWorld) world, pos);
        }
    }

    private BlockPos findMainBlock(World world, BlockPos pos) {
        BlockPos down = pos.down();
        if (world.getBlockState(down).isOf(ModBlocks.EYE_OF_HARMONY_OBELISK)) {
            return down;
        }
        BlockPos up = pos.up();
        if (world.getBlockState(up).isOf(ModBlocks.EYE_OF_HARMONY_OBELISK)) {
            return up;
        }
        return null;
    }

    private void destroyStructure(World world, BlockPos mainPos) {
        BlockPos up = mainPos.up();
        BlockPos down = mainPos.down();

        if (world.getBlockState(mainPos).isOf(ModBlocks.EYE_OF_HARMONY_OBELISK)) {
            world.removeBlock(mainPos, false);
        }
        if (world.getBlockState(up).isOf(ModBlocks.EYE_OF_HARMONY_PART)) {
            world.removeBlock(up, false);
        }
        if (world.getBlockState(down).isOf(ModBlocks.EYE_OF_HARMONY_PART)) {
            world.removeBlock(down, false);
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