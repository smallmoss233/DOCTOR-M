package doctor_m.util.creativity;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScytheChargingManager {
    public static final int MAX_CHARGE_LEVEL = 5;
    public static final int TICKS_PER_LEVEL = 40; // 2秒一层，满层10秒

    private static final Set<UUID> CHARGING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> CHARGE_LEVELS = new ConcurrentHashMap<>();

    public static void startCharging(PlayerEntity player) {
        CHARGING.add(player.getUuid());
        CHARGE_LEVELS.put(player.getUuid(), 0);
    }

    public static void stopCharging(PlayerEntity player) {
        CHARGING.remove(player.getUuid());
        CHARGE_LEVELS.remove(player.getUuid());
    }

    public static boolean isCharging(PlayerEntity player) {
        return CHARGING.contains(player.getUuid());
    }

    public static int getChargeLevel(PlayerEntity player) {
        return CHARGE_LEVELS.getOrDefault(player.getUuid(), 0);
    }

    public static void setChargeLevel(PlayerEntity player, int level) {
        CHARGE_LEVELS.put(player.getUuid(), level);
    }
}