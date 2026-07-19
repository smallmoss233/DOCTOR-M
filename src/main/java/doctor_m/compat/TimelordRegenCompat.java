package doctor_m.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TimelordRegenCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("timelordregen");

    private static Class<?> capableClass;
    private static Class<?> infoClass;
    private static Method isTimelordMethod;
    private static Method getInfoMethod;
    private static Method getUsesLeftMethod;
    private static Method setUsesLeftMethod;
    private static int maxRegenerations = 12; // 默认兜底

    static {
        if (LOADED) {
            try {
                capableClass = Class.forName("dev.amble.timelordregen.api.RegenerationCapable");
                infoClass    = Class.forName("dev.amble.timelordregen.api.RegenerationInfo");

                isTimelordMethod      = capableClass.getMethod("isTimelord");
                getInfoMethod         = capableClass.getMethod("getRegenerationInfo");
                getUsesLeftMethod     = infoClass.getMethod("getUsesLeft");
                setUsesLeftMethod     = infoClass.getMethod("setUsesLeft", int.class);

                Field maxField = infoClass.getField("MAX_REGENERATIONS");
                maxRegenerations = (int) maxField.get(null);
            } catch (Exception e) {
                // 反射失败就按未加载处理，避免崩溃
            }
        }
    }

    public static boolean isLoaded() {
        return LOADED && capableClass != null;
    }

    public static boolean isTimelord(PlayerEntity player) {
        if (!isLoaded()) return false;
        if (!capableClass.isInstance(player)) return false;
        try {
            return (boolean) isTimelordMethod.invoke(player);
        } catch (Exception e) {
            return false;
        }
    }

    public static RegenInfo getRegenInfo(PlayerEntity player) {
        if (!isLoaded()) return null;
        try {
            Object info = getInfoMethod.invoke(player);
            return info == null ? null : new RegenInfo(info);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getMaxRegenerations() {
        return maxRegenerations;
    }

    // 包装对象，避免主代码直接引用 timelordregen 类
    public static class RegenInfo {
        private final Object delegate;
        RegenInfo(Object delegate) { this.delegate = delegate; }

        public int getUsesLeft() {
            try { return (int) getUsesLeftMethod.invoke(delegate); }
            catch (Exception e) { return 0; }
        }

        public void setUsesLeft(int v) {
            try { setUsesLeftMethod.invoke(delegate, v); }
            catch (Exception e) { /* ignore */ }
        }
    }
}