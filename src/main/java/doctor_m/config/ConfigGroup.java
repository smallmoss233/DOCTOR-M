package doctor_m.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个配置字段所属的分组。
 * <p>值应为 {@link ConfigGroups} 里的标识符常量，
 * 显示名从语言文件 {@code gui.doctor_m.config.group.<id>} 读取。
 * <p>不写此注解的字段会归入 {@link ConfigGroups#MISC}。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigGroup {
    String value();
}