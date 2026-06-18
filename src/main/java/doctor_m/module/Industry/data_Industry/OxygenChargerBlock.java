package doctor_m.module.Industry.data_Industry;

import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.module.space.SpaceOxygenManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OxygenChargerBlock extends BlockWithEntity {

    public OxygenChargerBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenChargerBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;

        ItemStack held = player.getStackInHand(hand);

        // 处理氧气瓶
        if (held.getItem() instanceof OxygenTankItem) {
            if (OxygenTankItem.getOxygen(held) < OxygenTankItem.MAX_OXYGEN) {
                OxygenTankItem.setOxygen(held, OxygenTankItem.MAX_OXYGEN);
                player.sendMessage(Text.literal("§a氧气瓶已充满！"), true);
                player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
            } else {
                player.sendMessage(Text.literal("§a氧气瓶已满！"), true);
            }
            return ActionResult.SUCCESS;
        }

        // 处理宇航服胸甲
        if (held.getItem() instanceof SpacesuitItem && ((ArmorItem) held.getItem()).getType() == ArmorItem.Type.CHESTPLATE) {
            double current = SpaceOxygenManager.getOxygen(held);
            if (current < SpaceOxygenManager.MAX_OXYGEN) {
                SpaceOxygenManager.setOxygen(held, SpaceOxygenManager.MAX_OXYGEN);
                player.sendMessage(Text.literal("§a宇航服氧气已充满！"), true);
                player.playSound(SoundEvents.BLOCK_BELL_RESONATE, 1.0F, 1.0F);
            } else {
                player.sendMessage(Text.literal("§a宇航服氧气已满！"), true);
            }
            return ActionResult.SUCCESS;
        }

        // 其他物品不处理，静默返回
        return ActionResult.PASS;
    }
}