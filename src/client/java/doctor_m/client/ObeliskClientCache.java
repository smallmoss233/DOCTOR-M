package doctor_m.client;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class ObeliskClientCache {
    public static final Map<BlockPos, Float> ENTRIES = new HashMap<>();

    public static void update(BlockPos pos, float yOffset) {
        ENTRIES.put(pos.toImmutable(), yOffset);
    }
}