package doctor_m.block.data_block;

import doctor_m.block.entities.EyeOfHarmonyObeliskBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class EyeOfHarmonyObeliskBlock extends BlockWithEntity {

    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    // ← 关键：客户端在初始化时注入这个回调，main 只持有接口不引用具体类
    public static Consumer<EyeOfHarmonyObeliskBlockEntity> OPEN_SCREEN_CALLBACK = null;

    public EyeOfHarmonyObeliskBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EyeOfHarmonyObeliskBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
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
}