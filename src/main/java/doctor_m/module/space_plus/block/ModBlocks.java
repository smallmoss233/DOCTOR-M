package doctor_m.module.space_plus.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block OXYGEN_CHARGER = new OxygenChargerBlock(
            FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)
                    .requiresTool()
                    .nonOpaque()
    );

    public static final Block UNDERWATER_OXYGEN_GENERATOR = new UnderwaterOxygenGeneratorBlock(
            FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)
                    .requiresTool()
                    .nonOpaque()
    );

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier("doctor_m", "oxygen_charger"), OXYGEN_CHARGER);
        Registry.register(Registries.BLOCK, new Identifier("doctor_m", "underwater_oxygen_generator"), UNDERWATER_OXYGEN_GENERATOR);
    }
}