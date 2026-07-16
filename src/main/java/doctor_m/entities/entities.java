package doctor_m.entities;

import doctor_m.DOCTORM;
import doctor_m.entities.data.entity_103_tardis;
import doctor_m.entities.data.entity_103w_evereye;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class entities {

    public static final EntityType<entity_103_tardis> TYPE_103_TARDIS = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "type_103_tardis"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, entity_103_tardis::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build()
    );

    public static final EntityType<entity_103w_evereye> TYPE_103W_EVEREYE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(DOCTORM.MOD_ID, "type_103w_evereye"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, entity_103w_evereye::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .build()
    );

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(TYPE_103_TARDIS, entity_103_tardis.createMobAttributes());
        FabricDefaultAttributeRegistry.register(TYPE_103W_EVEREYE, entity_103w_evereye.createMobAttributes());
    }
}