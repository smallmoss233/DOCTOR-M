package doctor_m.CoffeeMachine;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class CoffeeMachineBlock extends Block implements BlockEntityProvider {
    // 添加方向属性
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    // 饮品ID到翻译键的映射
    private static final Map<String, String> DRINKS = new LinkedHashMap<>();

    static {
        // 饮品映射
        DRINKS.put("ait:water", "ait.item.drink.water");
        DRINKS.put("ait:iced_coffee", "ait.item.drink.iced_coffee");
        DRINKS.put("ait:hot_cocoa", "ait.item.drink.hot_cocoa");
        DRINKS.put("ait:vodka", "ait.item.drink.vodka");
        DRINKS.put("ait:coffee", "ait.item.drink.coffee");
        DRINKS.put("ait:latte", "ait.item.drink.latte");
        DRINKS.put("ait:chocolate_milk", "ait.item.drink.chocolate_milk");
        DRINKS.put("ait:tea", "ait.item.drink.tea");
        DRINKS.put("ait:milk", "ait.item.drink.milk");
    }

    public CoffeeMachineBlock(Settings settings) {
        super(settings
                .nonOpaque()  // 如果模型有透明部分，设置为非不透明
                .solidBlock((state, world, pos) -> false)  // 如果需要特殊碰撞箱
        );
        // 设置默认朝向
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // 放置方块时自动面向玩家
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof CoffeeMachineBlockEntity coffeeMachine)) {
            return ActionResult.FAIL;
        }

        // 如果咖啡机正在工作，只显示提示
        if (coffeeMachine.isWorking()) {
            player.sendMessage(Text.translatable("message.doctor_m.coffee_machine.working"), true);
            return ActionResult.SUCCESS;
        }

        // 获取玩家手中的物品
        ItemStack handStack = player.getStackInHand(hand);

        // 检查玩家手中是否有ait:mug
        boolean hasMug = false;

        if (!handStack.isEmpty()) {
            Identifier itemId = Registries.ITEM.getId(handStack.getItem());
            hasMug = "ait:mug".equals(itemId.toString());
        }

        if (hasMug) {
            // 玩家拿着ait:mug，开始制作当前饮品
            String targetDrinkId = getDrinkIdAtIndex(coffeeMachine.getCurrentDrinkIndex());
            String targetDrinkKey = DRINKS.get(targetDrinkId);

            // 移除一个杯子
            handStack.decrement(1);

            // 启动咖啡机工作
            coffeeMachine.startWorking(targetDrinkId);

            String targetDrinkName = Text.translatable(targetDrinkKey).getString();
            player.sendMessage(Text.translatable("message.doctor_m.coffee_machine.making",
                    targetDrinkName), true);
        } else {
            // 空手或手持其他物品，只切换到下一个饮品
            // 先获取当前饮品信息（切换前）
            String currentDrinkId = getDrinkIdAtIndex(coffeeMachine.getCurrentDrinkIndex());
            String currentDrinkKey = DRINKS.get(currentDrinkId);
            String currentDrinkName = Text.translatable(currentDrinkKey).getString();

            // 切换到下一个饮品
            coffeeMachine.nextDrink();

            // 获取切换后的饮品信息
            String newDrinkId = getDrinkIdAtIndex(coffeeMachine.getCurrentDrinkIndex());
            String newDrinkKey = DRINKS.get(newDrinkId);
            String newDrinkName = Text.translatable(newDrinkKey).getString();

            // 显示切换信息
            player.sendMessage(Text.translatable("message.doctor_m.coffee_machine.switched",
                    newDrinkName), true);
        }

        return ActionResult.SUCCESS;
    }

    private String getDrinkIdAtIndex(int index) {
        Object[] keys = DRINKS.keySet().toArray();
        return (String) keys[index % keys.length];
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CoffeeMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) {
            return null;
        }
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof CoffeeMachineBlockEntity coffeeMachine) {
                coffeeMachine.tick();
            }
        };
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // 这里可以定义3D模型的碰撞箱
        return VoxelShapes.cuboid(0.1, 0, 0.1, 0.9, 0.8, 0.9);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}