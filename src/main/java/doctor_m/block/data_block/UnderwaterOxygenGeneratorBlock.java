package doctor_m.block.data_block;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.block.ModBlockEntities;
import doctor_m.block.entities.UnderwaterOxygenGeneratorBlockEntity;
import doctor_m.config.ConfigManager;
import doctor_m.config.ModConfig;
import doctor_m.module.space_plus.OxygenSystem;
import doctor_m.module.space_plus.OxygenTankItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
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

public class UnderwaterOxygenGeneratorBlock extends BlockWithEntity {

    private static final int TICKS_PER_SECOND = 20;

    public UnderwaterOxygenGeneratorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new UnderwaterOxygenGeneratorBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) return null;
        return checkType(type, ModBlockEntities.UNDERWATER_OXYGEN_GENERATOR_ENTITY, UnderwaterOxygenGeneratorBlockEntity::tick);
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
        if (!(blockEntity instanceof UnderwaterOxygenGeneratorBlockEntity generator)) {
            return ActionResult.PASS;
        }

        ItemStack held = player.getStackInHand(hand);
        boolean isUnderwater = isSubmerged(world, pos);

        // 陆地模式：手持水桶右键 → 加水（每次提供 4 次充能配额）
        if (!isUnderwater && held.isOf(Items.WATER_BUCKET)) {
            int added = generator.addWaterCharges(4);
            if (added > 0) {
                player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.sendMessage(
                        Text.translatable("message.doctor_m.underwater_oxygen_generator.water_added"), true);
            } else {
                player.sendMessage(
                        Text.translatable("message.doctor_m.underwater_oxygen_generator.water_full"), true);
            }
            return ActionResult.SUCCESS;
        }

        // 检查冷却
        long currentTime = world.getTime();
        long cooldownEnd = generator.getCooldownEndTick();
        if (currentTime < cooldownEnd) {
            long remainingSeconds = (cooldownEnd - currentTime + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
            player.sendMessage(
                    Text.translatable("message.doctor_m.underwater_oxygen_generator.cooldown", remainingSeconds),
                    true
            );
            return ActionResult.SUCCESS;
        }

        // 充能逻辑
        ModConfig config = ConfigManager.getConfig();
        ChargeResult result = tryCharge(held, player, config, isUnderwater, generator);

        switch (result) {
            case SUCCESS -> {
                int cooldownSeconds = isUnderwater ? 10 : 20;
                int cooldownTicks = cooldownSeconds * TICKS_PER_SECOND;
                generator.setCooldownEndTick(currentTime + cooldownTicks);
                generator.markDirty();
                world.playSound(null, pos, SoundEvents.BLOCK_BELL_RESONATE,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
                return ActionResult.SUCCESS;
            }
            case NO_WATER -> {
                player.sendMessage(
                        Text.translatable("message.doctor_m.underwater_oxygen_generator.no_water"), true);
                return ActionResult.SUCCESS;
            }
            case ALREADY_FULL, INVALID_ITEM -> {
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.SUCCESS;
    }

    private boolean isSubmerged(World world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (world.getFluidState(neighbor).isIn(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private ChargeResult tryCharge(ItemStack held, PlayerEntity player, ModConfig config,
                                   boolean isUnderwater, UnderwaterOxygenGeneratorBlockEntity generator) {
        // 陆地模式检查是否有水
        if (!isUnderwater && generator.getWaterCharges() <= 0) {
            return ChargeResult.NO_WATER;
        }

        // 1) 氧气瓶
        if (held.getItem() instanceof OxygenTankItem) {
            double current = OxygenTankItem.getOxygen(held);
            double max = config.oxygenTankMaxOxygen;
            if (current < max) {
                double amount = isUnderwater ? max : max * 0.25;
                double newOxygen = Math.min(current + amount, max);
                OxygenTankItem.setOxygen(held, newOxygen);

                if (!isUnderwater) {
                    generator.consumeWaterCharge();
                }

                String key = isUnderwater
                        ? "message.doctor_m.underwater_oxygen_generator.tank_fill"
                        : "message.doctor_m.underwater_oxygen_generator.tank_fill_partial";
                player.sendMessage(Text.translatable(key), true);
                return ChargeResult.SUCCESS;
            }
            player.sendMessage(Text.translatable("message.doctor_m.underwater_oxygen_generator.tank_full"), true);
            return ChargeResult.ALREADY_FULL;
        }

        // 2) 航天服胸甲
        if (held.getItem() instanceof SpacesuitItem
                && held.getItem() instanceof ArmorItem armor
                && armor.getType() == ArmorItem.Type.CHESTPLATE) {
            double current = OxygenSystem.getOxygen(held);
            double max = config.spacesuitMaxOxygen;
            if (current < max) {
                double amount = isUnderwater ? max : max * 0.25;
                double newOxygen = Math.min(current + amount, max);
                OxygenSystem.setOxygen(held, newOxygen);

                if (!isUnderwater) {
                    generator.consumeWaterCharge();
                }

                String key = isUnderwater
                        ? "message.doctor_m.underwater_oxygen_generator.suit_fill"
                        : "message.doctor_m.underwater_oxygen_generator.suit_fill_partial";
                player.sendMessage(Text.translatable(key), true);
                return ChargeResult.SUCCESS;
            }
            player.sendMessage(Text.translatable("message.doctor_m.underwater_oxygen_generator.suit_full"), true);
            return ChargeResult.ALREADY_FULL;
        }

        // 3) 无效物品
        player.sendMessage(Text.translatable("message.doctor_m.underwater_oxygen_generator.invalid_item"), true);
        return ChargeResult.INVALID_ITEM;
    }

    private enum ChargeResult {
        SUCCESS,
        ALREADY_FULL,
        INVALID_ITEM,
        NO_WATER
    }
}