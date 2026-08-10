package doctor_m.block;

import doctor_m.block.data_block.EyeOfHarmonyObeliskBlock;
import doctor_m.block.data_block.OxygenChargerBlock;
import doctor_m.block.data_block.UnderwaterOxygenGeneratorBlock;
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

    // ← 加上这个
    public static final Block EYE_OF_HARMONY_OBELISK = new EyeOfHarmonyObeliskBlock(
            FabricBlockSettings.copyOf(Blocks.OBSIDIAN)
                    .requiresTool()
                    .nonOpaque()
                    .luminance(state -> 15)
    );

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier("doctor_m", "oxygen_charger"), OXYGEN_CHARGER);
        Registry.register(Registries.BLOCK, new Identifier("doctor_m", "underwater_oxygen_generator"), UNDERWATER_OXYGEN_GENERATOR);

        // ← 加上这个
        Registry.register(Registries.BLOCK, new Identifier("doctor_m", "eye_of_harmony_obelisk"), EYE_OF_HARMONY_OBELISK);
    }
}