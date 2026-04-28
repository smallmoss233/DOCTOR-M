package client.model;

import doctor_m.DOCTORM;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class model_layers {
    public static final EntityModelLayer TARDIS = new EntityModelLayer(new Identifier(DOCTORM.MOD_ID, "type103_tardis"), "main");
    public static final EntityModelLayer EVEREYE = new EntityModelLayer(new Identifier(DOCTORM.MOD_ID, "type103w_evereye"), "main");
}