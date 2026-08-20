package doctor_m.block.data_block;

import doctor_m.block.entities.ToyotaSpinningRotorBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class ToyotaSpinningRotorBlock extends Block implements BlockEntityProvider {

    public static final int VARIANT_COUNT = 3;
    public static final IntProperty VARIANT = IntProperty.of("variant", 0, VARIANT_COUNT - 1); // 0~2

    private static final VoxelShape CUBE = VoxelShapes.cuboid(0, 0, 0, 1, 1, 1);

    public ToyotaSpinningRotorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(VARIANT, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return CUBE;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);

        if (!(stack.getItem() instanceof DyeItem)) {
            return ActionResult.PASS;
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        int current = state.get(VARIANT);
        int next = (current + 1) % VARIANT_COUNT;
        world.setBlockState(pos, state.with(VARIANT, next));

        return ActionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull World world, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (!world.isClient) return null;
        return (world1, blockPos, blockState, ticker) -> {
            if (ticker instanceof ToyotaSpinningRotorBlockEntity entity) {
                entity.tick(world1, blockPos, blockState, entity);
            }
        };
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ToyotaSpinningRotorBlockEntity(pos, state);
    }
}