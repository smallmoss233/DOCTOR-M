package doctor_m.module.Industry.data_Industry;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import dev.amble.ait.module.planet.core.item.SpacesuitItem;
import doctor_m.module.space.SpaceOxygenManager;

public class OxygenChargerBlockEntity extends BlockEntity {

    public OxygenChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_CHARGER_ENTITY, pos, state); // 需先定义 ModBlockEntities
    }

    public static void tick(World world, BlockPos pos, BlockState state, OxygenChargerBlockEntity self) {
        if (world.isClient()) return;
        if (world.getTime() % 40 != 0) return; // 每2秒检测一次

        // 不检测封闭空间，只要方块存在就工作
        // 但可以增加节能：如果附近没有玩家则不工作（优化性能）
        boolean hasPlayerNearby = !world.getEntitiesByClass(PlayerEntity.class,
                new Box(pos).expand(8), e -> !e.isSpectator() && e.isAlive()).isEmpty();
        if (!hasPlayerNearby) return;

        // 为范围内玩家补充氧气（半径5格）
        int radius = 5;
        Box box = new Box(
                pos.add(-radius + 1, -radius + 1, -radius + 1),
                pos.add(radius - 1, radius - 1, radius - 1)
        );
        world.getEntitiesByClass(PlayerEntity.class, box, e -> !e.isSpectator() && e.isAlive())
                .forEach(player -> {
                    ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
                    if (chest.getItem() instanceof SpacesuitItem) {
                        double current = SpaceOxygenManager.getOxygen(chest);
                        if (current < SpaceOxygenManager.MAX_OXYGEN) {
                            SpaceOxygenManager.refillOxygen(chest, 0.5);
                        }
                    }
                });
    }
}