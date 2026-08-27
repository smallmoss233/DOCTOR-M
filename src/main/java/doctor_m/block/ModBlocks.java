package doctor_m.block;

import doctor_m.DOCTORM;
import doctor_m.block.data_block.*;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
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

    public static final Block EYE_OF_HARMONY_OBELISK = new EyeOfHarmonyObeliskBlock(
            FabricBlockSettings.copyOf(Blocks.OBSIDIAN)
                    .requiresTool()
                    .nonOpaque()
                    .luminance(state -> 15)
    );

    public static final Block EYE_OF_HARMONY_PART = new EyeOfHarmonyPartBlock(
            FabricBlockSettings.copyOf(Blocks.OBSIDIAN)
                    .dropsNothing()
                    .nonOpaque()
                    .luminance(state -> 0)
    );

    public static final Block TOYOTA_SPINNING_ROTOR = new ToyotaSpinningRotorBlock(
            FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)
                    .requiresTool()
                    .nonOpaque()
    );

    public static final Block DOLL_JIN_MARY = new DollBlock(
            FabricBlockSettings.create()
                    .nonOpaque()
                    .strength(0.0f, 0.0f)
                    .sounds(BlockSoundGroup.WOOL)
    );

    public static final Block DOLL_SMALLMOSS_OLD = new DollBlock(
            FabricBlockSettings.create()
                    .nonOpaque()
                    .strength(0.0f, 0.0f)
                    .sounds(BlockSoundGroup.WOOL)
    );

    public static void register() {
        Registry.register(Registries.BLOCK, new Identifier(DOCTORM.MOD_ID, "oxygen_charger"), OXYGEN_CHARGER);
        Registry.register(Registries.BLOCK, new Identifier(DOCTORM.MOD_ID, "underwater_oxygen_generator"), UNDERWATER_OXYGEN_GENERATOR);
        Registry.register(Registries.BLOCK, new Identifier(DOCTORM.MOD_ID, "eye_of_harmony_obelisk"), EYE_OF_HARMONY_OBELISK);
        Registry.register(Registries.BLOCK, new Identifier(DOCTORM.MOD_ID, "eye_of_harmony_part"), EYE_OF_HARMONY_PART);
        Registry.register(Registries.BLOCK, new Identifier(DOCTORM.MOD_ID, "toyota_spinning_rotor"), TOYOTA_SPINNING_ROTOR);
        Registry.register(Registries.BLOCK, new Identifier(DOCTORM.MOD_ID, "doll_jin_mary"), DOLL_JIN_MARY);
        Registry.register(Registries.BLOCK, new Identifier(DOCTORM.MOD_ID, "doll_smallmoss_old"), DOLL_SMALLMOSS_OLD);
    }
}