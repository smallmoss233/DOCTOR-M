package doctor_m.block.data_block;

import doctor_m.block.entities.SarWorkBenchBlockEntity;
import doctor_m.menu.SARGUI;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SarWorkBenchBlock extends Block implements BlockEntityProvider {
    public  SarWorkBenchBlock(Settings settings){
        super(settings);
    }
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
        }
        return ActionResult.SUCCESS;
    }
    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SarWorkBenchBlockEntity workbenchEntity) {
            return new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, player) -> new SARGUI(syncId, inv, workbenchEntity),
                    Text.translatable("block.doctor_m.workbench")
            );
        }
        return null;
    }
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SarWorkBenchBlockEntity(pos, state);
    }
}
