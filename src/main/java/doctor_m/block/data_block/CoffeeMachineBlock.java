package doctor_m.block.data_block;

import dev.amble.ait.core.AITItems;
import dev.amble.ait.core.drinks.Drink;
import dev.amble.ait.core.drinks.DrinkRegistry;
import dev.amble.ait.core.drinks.DrinkUtil;
import doctor_m.block.ModBlockEntities;
import doctor_m.block.entities.CoffeeMachineBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoffeeMachineBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.createCuboidShape(3.0, 0.0, 2.0, 13.0, 15.0, 15.0);

    public CoffeeMachineBlock(Settings settings) {
        super(settings.nonOpaque());
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof CoffeeMachineBlockEntity machine)) return ActionResult.FAIL;

        if (machine.isWorking()) {
            player.sendMessage(Text.translatable("tooltip.doctor_m.coffee_machine.working"), true);
            return ActionResult.SUCCESS;
        }

        List<Drink> drinks = DrinkRegistry.getInstance().toList();
        if (drinks.isEmpty()) return ActionResult.FAIL;

        ItemStack handStack = player.getStackInHand(hand);
        Drink current = drinks.get(machine.getCurrentDrinkIndex());

        if (isMug(handStack)) {
            handStack.decrement(1);
            machine.startWorking(machine.getCurrentDrinkIndex());
            player.sendMessage(Text.translatable("tooltip.doctor_m.coffee_machine.making",
                    getDrinkName(current)), true);
        } else {
            machine.nextDrink();
            Drink next = drinks.get(machine.getCurrentDrinkIndex());
            player.sendMessage(Text.translatable("tooltip.doctor_m.coffee_machine.switched",
                    getDrinkName(next)), true);
        }

        return ActionResult.SUCCESS;
    }

    private static boolean isMug(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return Registries.ITEM.getId(stack.getItem()).equals(new Identifier("ait", "mug"));
    }

    private static String getDrinkName(Drink drink) {
        ItemStack preview = DrinkUtil.setDrink(new ItemStack(AITItems.MUG), drink);
        return preview.getName().getString();
    }

    public static int getDrinkCount() {
        return DrinkRegistry.getInstance().size();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CoffeeMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, ModBlockEntities.COFFEE_MACHINE, CoffeeMachineBlockEntity::tick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(
            BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == given ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}