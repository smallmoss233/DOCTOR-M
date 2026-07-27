package doctor_m.dimension;

import doctor_m.DOCTORM;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class DimensionRegister {

    public static void register() {
        Registry.register(
                Registries.CHUNK_GENERATOR,
                new Identifier(DOCTORM.MOD_ID, "titan"),
                TitanChunkGenerator.CODEC
        );
    }
}