package doctor_m.handler.KeytoTime;

import net.minecraft.entity.LivingEntity;

public class HealthGuard {
    private static final ThreadLocal<Boolean> allowHealthWrite = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> allowMaxHealthWrite = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<LivingEntity> attributeAccessEntity = new ThreadLocal<>();

    public static boolean isHealthWriteAllowed() {
        return allowHealthWrite.get();
    }

    public static boolean isMaxHealthWriteAllowed() {
        return allowMaxHealthWrite.get();
    }

    public static void withHealthWrite(Runnable action) {
        allowHealthWrite.set(true);
        try {
            action.run();
        } finally {
            allowHealthWrite.set(false);
        }
    }

    public static void withMaxHealthWrite(Runnable action) {
        allowMaxHealthWrite.set(true);
        try {
            action.run();
        } finally {
            allowMaxHealthWrite.set(false);
        }
    }
}