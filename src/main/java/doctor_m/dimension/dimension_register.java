package doctor_m.dimension;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import doctor_m.DOCTORM;

public class dimension_register {

    public static void register() {
        Registry.register(
                Registries.CHUNK_GENERATOR,
                new Identifier(DOCTORM.MOD_ID, "titan"),
                TitanChunkGenerator.CODEC
        );
    }
}