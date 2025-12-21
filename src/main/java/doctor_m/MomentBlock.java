package doctor_m.block; // 建议按功能分包

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class MomentBlock extends Block {
    // 定义方块的碰撞箱和轮廓箱（这是实现“3D模型”的关键之一）
    // 例如，这里创建了一个从地面到屋顶的完整方块轮廓
    private static final VoxelShape SHAPE = VoxelShapes.fullCube();

    public MomentBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // 返回我们定义的形状。如果你想做成非完整方块（如楼梯），在此处修改SHAPE即可。
        return SHAPE;
    }
}