package doctor_m.block.data_block;

import doctor_m.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class EyeOfHarmonyPartBlock extends Block {
    public EyeOfHarmonyPartBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Block.createCuboidShape(0, 0, 0, 16, 16, 16); // 完整碰撞箱
    }

    @Override
    public String getTranslationKey() {
        return ModBlocks.EYE_OF_HARMONY_OBELISK.getTranslationKey();
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // 被活塞推动时，不触发结构破坏
        if (moved) return;

        // 真正被破坏时（不是被替换）
        if (!state.isOf(newState.getBlock())) {
            BlockPos mainPos = findMainBlock(world, pos);
            if (mainPos != null) {
                destroyStructure(world, mainPos);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
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
}