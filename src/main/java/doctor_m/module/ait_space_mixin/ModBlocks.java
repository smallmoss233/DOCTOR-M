package doctor_m.module.ait_space_mixin;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block OXYGEN_CHARGER = new OxygenChargerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque());

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier("doctor_m", "oxygen_charger"), OXYGEN_CHARGER);
    }
}