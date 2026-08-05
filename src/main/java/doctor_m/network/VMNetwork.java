package doctor_m.network;

import doctor_m.DOCTORM;
import net.minecraft.util.Identifier;

public class VMNetwork {
    public static final Identifier CYCLE_DIM = DOCTORM.id("vm/cycle_dim");
    public static final Identifier SET_CURRENT_DEST = DOCTORM.id("vm/set_current");
    public static final Identifier SET_PREV_DEST = DOCTORM.id("vm/set_prev");
    public static final Identifier TELEPORT = DOCTORM.id("vm/teleport");
}