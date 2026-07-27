package doctor_m.entities;

import doctor_m.DOCTORM;
import doctor_m.entities.data.Entity103Tardis;
import doctor_m.entities.data.Entity103wEvereye;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Entities {

    public static final EntityType<Entity103Tardis> TYPE_103_TARDIS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "type_103_tardis"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, Entity103Tardis::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build()
    );

    public static final EntityType<Entity103wEvereye> TYPE_103W_EVEREYE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "type_103w_evereye"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, Entity103wEvereye::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build()
    );

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(TYPE_103_TARDIS, Entity103Tardis.createMobAttributes());
        FabricDefaultAttributeRegistry.register(TYPE_103W_EVEREYE, Entity103wEvereye.createMobAttributes());
    }
}