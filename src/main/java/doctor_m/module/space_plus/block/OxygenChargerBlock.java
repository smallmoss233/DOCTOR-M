package doctor_m.module.space_plus.block;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.config.ConfigManager;
import doctor_m.module.space_plus.OxygenTankItem;
import doctor_m.module.space_plus.system.OxygenSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OxygenChargerBlock extends BlockWithEntity {

    public OxygenChargerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenChargerBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        var config = ConfigManager.getConfig();
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof OxygenChargerBlockEntity charger)) {
            return ActionResult.PASS;
        }

        long currentTime = world.getTime();
        if (currentTime < charger.getCooldownEndTick()) {
            long remainingSeconds = (charger.getCooldownEndTick() - currentTime) / 20;
            player.sendMessage(
                    Text.translatable("message.doctor_m.oxygen_charger.cooldown", remainingSeconds),
                    true
            );
            return ActionResult.SUCCESS;
        }

        ItemStack held = player.getStackInHand(hand);
        boolean charged = false;

        // 获取最大容量
        double maxTankOxygen = config.oxygenTankMaxOxygen;
        double maxSuitOxygen = config.spacesuitMaxOxygen;

        // 尝试充氧气瓶
        if (held.getItem() instanceof OxygenTankItem) {
            double current = OxygenTankItem.getOxygen(held);
            if (current < maxTankOxygen) {
                OxygenTankItem.setOxygen(held, maxTankOxygen);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_fill"), true);
                charged = true;
            } else {
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_full"), true);
                return ActionResult.SUCCESS;
            }
        }
        // 尝试充航天服（胸甲）
        else if (held.getItem() instanceof SpacesuitItem && ((ArmorItem) held.getItem()).getType() == ArmorItem.Type.CHESTPLATE) {
            double current = OxygenSystem.getOxygen(held);
            if (current < maxSuitOxygen) {
                OxygenSystem.setOxygen(held, maxSuitOxygen);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_fill"), true);
                charged = true;
            } else {
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_full"), true);
                return ActionResult.SUCCESS;
            }
        } else {
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.invalid_item"), true);
            return ActionResult.SUCCESS;
        }

        if (charged) {
            int cooldownSeconds = config.oxygenChargerCooldownSeconds;
            charger.setCooldownEndTick(world.getTime() + cooldownSeconds * 20L);
            charger.markDirty();
            player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
        }

        return ActionResult.SUCCESS;
    }
}