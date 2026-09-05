package doctor_m.block.data_block;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.block.entities.OxygenChargerBlockEntity;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.module.space_plus.OxygenSystem;
import doctor_m.module.space_plus.Tank.JetOxygenTankItem;
import doctor_m.module.space_plus.Tank.OxygenTankItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
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

    private static final int TICKS_PER_SECOND = 20;

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

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof OxygenChargerBlockEntity charger)) {
            return ActionResult.PASS;
        }

        long currentTime = world.getTime();
        long cooldownEnd = charger.getCooldownEndTick();
        if (currentTime < cooldownEnd) {
            // 修复：向上取整，避免剩余 1~19 tick 时显示 "0 秒"
            long remainingSeconds = (cooldownEnd - currentTime + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
            player.sendMessage(
                    Text.translatable("message.doctor_m.oxygen_charger.cooldown", remainingSeconds),
                    true
            );
            return ActionResult.SUCCESS;
        }

        ModConfig config = ConfigManager.getConfig();
        ChargeResult result = tryCharge(player.getStackInHand(hand), player, config);

        switch (result) {
            case SUCCESS -> {
                int cooldownTicks = config.oxygenChargerCooldownSeconds * TICKS_PER_SECOND;
                charger.setCooldownEndTick(currentTime + cooldownTicks);
                charger.markDirty();
                // 优化：改为环境音效，附近玩家都能听到
                world.playSound(null, pos, SoundEvents.BLOCK_BELL_RESONATE,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
                return ActionResult.SUCCESS;
            }
            case ALREADY_FULL, INVALID_ITEM -> {
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.SUCCESS;
    }

    /**
     * 尝试为手持物品充能，返回操作结果。
     * 内部已处理玩家提示，调用方只需根据结果设置冷却。
     */
    private ChargeResult tryCharge(ItemStack held, PlayerEntity player, ModConfig config) {
        // 1) 普通氧气瓶及其子类（高级、超级）
        if (held.getItem() instanceof OxygenTankItem tankItem) {
            double current = OxygenTankItem.getOxygen(held);
            double max = tankItem.getMaxOxygen();
            if (current < max) {
                OxygenTankItem.setOxygen(held, max);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_fill"), true);
                return ChargeResult.SUCCESS;
            }
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_full"), true);
            return ChargeResult.ALREADY_FULL;
        }

        // 2) 喷气氧气瓶（独立类，单独处理）
        if (held.getItem() instanceof JetOxygenTankItem jetTank) {
            double current = jetTank.getOxygen(held);
            double max = jetTank.getMaxOxygen();
            if (current < max) {
                jetTank.setOxygen(held, max);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_fill"), true);
                return ChargeResult.SUCCESS;
            }
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.tank_full"), true);
            return ChargeResult.ALREADY_FULL;
        }

        // 3) 航天服胸甲
        if (held.getItem() instanceof SpacesuitItem
                && held.getItem() instanceof ArmorItem armor
                && armor.getType() == ArmorItem.Type.CHESTPLATE) {
            double current = OxygenSystem.getOxygen(held);
            double max = config.spacesuitMaxOxygen;
            if (current < max) {
                OxygenSystem.setOxygen(held, max);
                player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_fill"), true);
                return ChargeResult.SUCCESS;
            }
            player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.suit_full"), true);
            return ChargeResult.ALREADY_FULL;
        }

        // 4) 无效物品
        player.sendMessage(Text.translatable("message.doctor_m.oxygen_charger.invalid_item"), true);
        return ChargeResult.INVALID_ITEM;
    }

    private enum ChargeResult {
        SUCCESS,
        ALREADY_FULL,
        INVALID_ITEM
    }
}