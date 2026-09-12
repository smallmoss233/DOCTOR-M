package doctor_m.api;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 基于「字段名 = 注册 ID」约定的自动注册工具。
 *
 * <p>支持：物品、方块（可选自带 BlockItem）、方块实体、实体。
 *
 * <p>使用示例：
 * <pre>{@code
 * public class ModBlocks {
 *     public static final Block FOO = new Block(...);
 *
 *     @AutoRegister.NoItem  // 不生成 BlockItem
 *     public static final Block BAR = new Block(...);
 *
 *     @AutoRegister.Id("custom_id")  // 自定义注册 ID
 *     public static final Block BAZ = new Block(...);
 *
 *     public static void register() {
 *         AutoRegister.blocksWithItems(ModBlocks.class, "doctor_m");
 *     }
 * }
 * }</pre>
 */
public final class AutoRegister {

    private AutoRegister() {}

    // ==================== 注解 ====================

    /** 标记在 Block 字段上：只注册方块，不生成对应的 BlockItem。 */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface NoItem {}

    /** 显式指定注册 ID，覆盖默认的 field.getName().toLowerCase()。 */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Id {
        String value();
    }

    // ==================== 公开注册入口 ====================

    /**
     * 注册类中所有 public static final Item 字段。
     * 扫描完成后会自动调用目标类的静态方法 registerAbilities()（若存在）。
     */
    public static void items(Class<?> clazz, String modId) {
        scan(clazz, modId, Item.class, Registries.ITEM);
        tryInvokePostRegister(clazz);
    }

    /** 注册类中所有 public static final Block 字段（不生成 BlockItem）。 */
    public static void blocks(Class<?> clazz, String modId) {
        scan(clazz, modId, Block.class, Registries.BLOCK);
    }

    /**
     * 注册类中所有 public static final Block 字段，并为其生成 BlockItem。
     * 使用默认的 Item.Settings。
     */
    public static void blocksWithItems(Class<?> clazz, String modId) {
        blocksWithItems(clazz, modId, new Item.Settings());
    }

    /*
     注册类中所有 public static final Block 字段，并为其生成 BlockItem
     <p>使用原版 {@link Items#register(Block, Item.Settings)} 完成物品注册
     它会同时填充内部映射，确保 {@code block.asItem()} 返回同一个实例，
     因此 {@code ModBlocks.FOO.asItem() == 注册物品} 恒成立。
     <p>标注了 {@link NoItem} 的字段仅注册方块，不生成物品。
     */

    public static void blocksWithItems(Class<?> clazz, String modId, Item.Settings itemSettings) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!isValid(field, Block.class)) continue;
            Block block = getStaticField(field, Block.class);

            String id = resolveId(field);
            Registry.register(Registries.BLOCK, new Identifier(modId, id), block);

            if (!field.isAnnotationPresent(NoItem.class)) {
                BlockItem blockItem = new BlockItem(block, itemSettings);
                Items.register(block, blockItem);
            }
        }
    }

    /** 注册类中所有 public static final EntityType 字段。 */
    public static void entities(Class<?> clazz, String modId) {
        scan(clazz, modId, EntityType.class, Registries.ENTITY_TYPE);
    }

    /** 注册类中所有 public static final BlockEntityType 字段。 */
    public static void blockEntities(Class<?> clazz, String modId) {
        scan(clazz, modId, BlockEntityType.class, Registries.BLOCK_ENTITY_TYPE);
    }

    // ==================== 私有工具 ====================

    /**
     * 通用扫描：避免泛型通配符与 Registry&lt;T&gt; 的类型冲突，
     * 使用原始类型 + 手动类型检查。类型安全由 {@link #isValid} 保证。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void scan(Class<?> clazz, String modId, Class<?> type, Registry registry) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!isValid(field, type)) continue;
            Object instance = getStaticField(field, type);
            Registry.register(registry, new Identifier(modId, resolveId(field)), instance);
        }
    }

    /** 读取静态字段，若为 null 则抛出带有明确提示的异常。 */
    private static <T> T getStaticField(Field field, Class<T> type) {
        try {
            Object value = field.get(null);
            if (value == null) {
                throw new IllegalStateException("Field " + field.getName()
                        + " is null. AutoRegister must be called AFTER field initialization.");
            }
            return type.cast(value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to read field: " + field.getName(), e);
        }
    }

    /** 解析注册 ID：优先读取 {@link Id} 注解，否则将字段名转小写。 */
    private static String resolveId(Field field) {
        Id annotation = field.getAnnotation(Id.class);
        return annotation != null ? annotation.value() : field.getName().toLowerCase();
    }

    /** 若目标类定义了静态方法 registerAbilities()，自动调用它。 */
    private static void tryInvokePostRegister(Class<?> clazz) {
        Method method;
        try {
            method = clazz.getDeclaredMethod("registerAbilities");
        } catch (NoSuchMethodException e) {
            return; // 没有这个方法很正常，直接返回
        }

        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException(
                    "registerAbilities() must be static in " + clazz.getName());
        }

        try {
            method.setAccessible(true);
            method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to invoke registerAbilities() on " + clazz.getName(), e);
        }
    }

    /**
     * 判断字段是否符合自动注册的约定：
     * public static final + 类型匹配。
     */
    private static boolean isValid(Field field, Class<?> expectedType) {
        int mod = field.getModifiers();
        return Modifier.isPublic(mod)
                && Modifier.isStatic(mod)
                && Modifier.isFinal(mod)
                && expectedType.isAssignableFrom(field.getType());
    }
}