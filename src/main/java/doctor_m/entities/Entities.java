package doctor_m.entities;

import doctor_m.DOCTORM;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Entities {
    public static final EntityType<entity_tardis> TYPE_103_TARDIS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "type_103_tardis"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, entity_tardis::new)
                    .dimensions(EntityDimensions.fixed(0.75f, 0.75f))
                    .build()
    );
    public static final EntityType<entity_tardis> TYPE_103W_EVEREYE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "type_103w_evereye"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, entity_tardis::new)
                    .dimensions(EntityDimensions.fixed(0.75f, 0.75f))
                    .build()
    );

    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(TYPE_103_TARDIS, entity_tardis.createMobAttributes());
        FabricDefaultAttributeRegistry.register(TYPE_103W_EVEREYE, entity_tardis.createMobAttributes());
    }

    public static void register() {
    }
}