package doctor_m.block;

import doctor_m.DOCTORM;
import mosslib.api.AutoRegister;
import doctor_m.block.data_block.*;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.sound.BlockSoundGroup;

public class ModBlocks {

    public static final Block OXYGEN_CHARGER = new OxygenChargerBlock(
            FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).requiresTool().nonOpaque());

    public static final Block UNDERWATER_OXYGEN_GENERATOR = new UnderwaterOxygenGeneratorBlock(
            FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).requiresTool().nonOpaque());

    public static final Block EYE_OF_HARMONY_OBELISK = new EyeOfHarmonyObeliskBlock(
            FabricBlockSettings.copyOf(Blocks.OBSIDIAN).requiresTool().nonOpaque());

    /** 结构零件，不生成物品 */
    @AutoRegister.NoItem
    public static final Block EYE_OF_HARMONY_PART = new EyeOfHarmonyPartBlock(
            FabricBlockSettings.copyOf(Blocks.OBSIDIAN).dropsNothing().nonOpaque());

    public static final Block TOYOTA_SPINNING_ROTOR = new ToyotaSpinningRotorBlock(
            FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).requiresTool().nonOpaque());

    public static final Block DOLL_JIN_MARY = new DollBlock(dollSettings());
    public static final Block DOLL_SMALLMOSS_OLD = new DollBlock(dollSettings());
    public static final Block DOLL_TC020 = new DollBlock(dollSettings());
    public static final Block DOLL_ASDJDFK = new DollBlock(dollSettings());
    public static final Block DOLL_SIGEERTE = new DollBlock(dollSettings());
    public static final Block DOLL_TSINAFS_BCIM = new DollBlock(dollSettings());
    public static final Block DOLL_ASNIT_PNQING = new DollBlock(dollSettings());
    public static final Block DOLL_TIANX = new DollBlock(dollSettings());
    public static final Block DOLL_KILIN_MUS = new DollBlock(dollSettings());
    public static final Block DOLL_JOGGEST = new DollBlock(dollSettings());
    public static final Block DOLL_NX_SEEKER = new DollBlock(dollSettings());

    public static final Block COFFEE_MACHINE = new CoffeeMachineBlock(
            AbstractBlock.Settings.create().strength(1.5f, 6.0f));

    private static FabricBlockSettings dollSettings() {
        return FabricBlockSettings.create()
                .nonOpaque()
                .strength(0.0f, 0.0f)
                .sounds(BlockSoundGroup.WOOL);
    }

    public static void register() {
        AutoRegister.blocksWithItems(ModBlocks.class, DOCTORM.MOD_ID);
    }
}