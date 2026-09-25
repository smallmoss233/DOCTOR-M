package doctor_m.client.Config;

import doctor_m.config.ConfigGroup;
import doctor_m.config.ConfigGroups;
import doctor_m.config.ModConfig;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConfigScreenBuilder {

    public static DOCTORMConfigScreen build(Screen parent, ModConfig config) {
        DOCTORMConfigScreen screen = new DOCTORMConfigScreen(parent);

        // 默认实例，作为"重置"目标
        ModConfig defaults = new ModConfig();

        for (Field field : ModConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;

            // ★ 直接读注解，不再靠字段名前缀猜
            ConfigGroup ann = field.getAnnotation(ConfigGroup.class);
            String category = (ann != null) ? ann.value() : ConfigGroups.MISC;

            ConfigEntry entry = buildEntry(field, config, defaults, category);
            if (entry != null) {
                screen.addEntry(entry);
            }
        }

        return screen;
    }

    private static ConfigEntry buildEntry(Field field, ModConfig config,
                                          ModConfig defaults, String category) {
        try {
            field.setAccessible(true);
            Object currentValue = field.get(config);
            Object defaultValue = field.get(defaults);
            if (currentValue == null || defaultValue == null) return null;

            String fieldName = field.getName();
            String fallback = humanize(fieldName);
            Class<?> type = field.getType();

            // 布尔
            if (type == boolean.class || type == Boolean.class) {
                return new ConfigEntry.BoolEntry(fieldName, fallback, category,
                        (boolean) currentValue, (boolean) defaultValue,
                        v -> write(field, config, v));
            }

            // 整数（含 long / short / byte）
            if (type == int.class || type == long.class
                    || type == short.class || type == byte.class) {
                long cur = ((Number) currentValue).longValue();
                long def = ((Number) defaultValue).longValue();
                long step = pickIntStep(fieldName, cur);
                return new ConfigEntry.NumberEntry(fieldName, fallback, category,
                        cur, def, Long.MIN_VALUE / 4, Long.MAX_VALUE / 4, step, true,
                        d -> write(field, config, (long) d));
            }

            // 浮点
            if (type == double.class || type == float.class) {
                double cur = ((Number) currentValue).doubleValue();
                double def = ((Number) defaultValue).doubleValue();
                double step = pickDoubleStep(fieldName, cur);
                return new ConfigEntry.NumberEntry(fieldName, fallback, category,
                        cur, def, -1e9, 1e9, step, false,
                        d -> write(field, config, d));
            }

            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static long pickIntStep(String name, long v) {
        String n = name.toLowerCase();
        if (n.contains("ticks") || n.contains("second")) return 1;
        if (n.contains("perdamage") || n.contains("pertick")) return 1;
        if (n.contains("multiplier") || n.contains("ratio")) return 1;
        if (n.contains("radius")) return 1;
        if (n.contains("step")) return 1;
        if (n.contains("flags")) return 1;
        if (n.contains("energy") || n.contains("fuel") || n.contains("capacity")
                || n.contains("oxygen")) {
            long a = Math.abs(v);
            if (a >= 1000) return 100;
            if (a >= 100) return 10;
            return 1;
        }
        long a = Math.abs(v);
        if (a >= 10000) return 1000;
        if (a >= 1000) return 100;
        if (a >= 100) return 10;
        return 1;
    }

    private static double pickDoubleStep(String name, double v) {
        String n = name.toLowerCase();
        if (n.contains("multiplier") || n.contains("ratio")
                || n.contains("strength") || n.contains("force")
                || n.contains("inertia") || n.contains("compensation")
                || n.contains("consumemultiplier")
                || n.contains("saturation")) {
            return 0.05;
        }
        if (n.contains("radius") || n.contains("size")
                || n.contains("distance") || n.contains("reach")) {
            return 0.5;
        }
        if (n.contains("speed") || n.contains("thrust")) {
            return 0.5;
        }
        if (n.contains("oxygen") || n.contains("energy") || n.contains("fuel")) {
            return 50.0;
        }
        double a = Math.abs(v);
        if (a >= 1000) return 100;
        if (a >= 100) return 10;
        if (a >= 10) return 1;
        if (a >= 1) return 0.1;
        if (a >= 0.1) return 0.05;
        return 0.01;
    }

    private static String humanize(String fieldName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) sb.append(' ');
            sb.append(c);
        }
        String s = sb.toString();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void write(Field field, Object target, Object value) {
        try {
            Class<?> type = field.getType();
            if (type == int.class)         field.set(target, ((Number) value).intValue());
            else if (type == long.class)   field.set(target, ((Number) value).longValue());
            else if (type == short.class)  field.set(target, ((Number) value).shortValue());
            else if (type == byte.class)   field.set(target, ((Number) value).byteValue());
            else if (type == double.class) field.set(target, ((Number) value).doubleValue());
            else if (type == float.class)  field.set(target, ((Number) value).floatValue());
            else                            field.set(target, value);
        } catch (IllegalAccessException ignored) {}
    }
}