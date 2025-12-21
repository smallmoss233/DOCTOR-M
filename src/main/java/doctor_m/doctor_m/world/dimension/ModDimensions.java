package doctor_m.world.dimension;

import doctor_m.DOCTORM;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ModDimensions {
    // 定义维度的RegistryKey，这是维度在代码中的唯一标识符
    public static final RegistryKey<DimensionType> TRENZALORE_DIM_TYPE = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(DOCTORM.MOD_ID, "trenzalore")
    );
    public static final RegistryKey<World> TRENZALORE_WORLD = RegistryKey.of(
            RegistryKeys.WORLD,
            new Identifier(DOCTORM.MOD_ID, "trenzalore")
    );

    public static void register() {
        // 这个方法用于在模组初始化时调用，可以留空或用于未来注册
        DOCTORM.LOGGER.info("Registering ModDimensions for " + DOCTORM.MOD_ID);
    }
}