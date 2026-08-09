package doctor_m.api;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class AutoRegister {

    public static void items(Class<?> clazz, String modId) {
        scan(clazz, modId, Item.class, Registries.ITEM);
        tryInvokePostRegister(clazz);
    }

    public static void blocks(Class<?> clazz, String modId) {
        scan(clazz, modId, Block.class, Registries.BLOCK);
    }

    public static void entities(Class<?> clazz, String modId) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!isValid(field, EntityType.class)) continue;

            String id = field.getName().toLowerCase();
            try {
                EntityType<?> instance = (EntityType<?>) field.get(null);
                if (instance == null) {
                    throw new IllegalStateException("Field " + field.getName()
                            + " is null. Make sure AutoRegister is called AFTER field initialization.");
                }
                Registry.register(Registries.ENTITY_TYPE, new Identifier(modId, id), instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to auto-register field: " + field.getName(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void scan(Class<?> clazz, String modId, Class<T> type, Registry<T> registry) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!isValid(field, type)) continue;

            String id = field.getName().toLowerCase();
            try {
                T instance = (T) field.get(null);
                if (instance == null) {
                    throw new IllegalStateException("Field " + field.getName()
                            + " is null. Make sure AutoRegister is called AFTER field initialization.");
                }
                Registry.register(registry, new Identifier(modId, id), instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to auto-register field: " + field.getName(), e);
            }
        }
    }

    /**
     * 自动调用目标类的 registerAbilities() 静态方法（如果有的话）。
     * 这样主类里不需要再手动写 items.registerAbilities();
     */
    private static void tryInvokePostRegister(Class<?> clazz) {
        try {
            Method method = clazz.getDeclaredMethod("registerAbilities");
            if (Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                method.invoke(null);
            }
        } catch (NoSuchMethodException e) {
            // 没有 registerAbilities 方法，正常，直接跳过
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke registerAbilities() on " + clazz.getName(), e);
        }
    }

    private static boolean isValid(Field field, Class<?> expectedType) {
        int mod = field.getModifiers();
        return Modifier.isPublic(mod)
                && Modifier.isStatic(mod)
                && Modifier.isFinal(mod)
                && expectedType.isAssignableFrom(field.getType());
    }
}