package doctor_m.command;

import net.minecraft.util.Identifier;

public class IdMappingUtil {
    private static final String SEPARATOR = "__";
    private static final char PATH_SEPARATOR = '/';
    private static final char MAPPING_SEPARATOR = '-';

    /**
     * 将 Identifier 转为映射名，供补全建议使用
     */
    public static String toMapping(Identifier id) {
        String path = id.getPath().replace(PATH_SEPARATOR, MAPPING_SEPARATOR);
        return id.getNamespace() + SEPARATOR + path;
    }

    /**
     * 将用户输入的映射名转为 Identifier
     * 优先按 __ 分割，若没有 __ 则尝试按标准 namespace:path 解析（兜底）
     */
    public static Identifier fromMapping(String input) {
        if (input == null || input.isEmpty()) return null;

        int idx = input.indexOf(SEPARATOR);
        if (idx >= 0) {
            String namespace = input.substring(0, idx);
            String path = input.substring(idx + SEPARATOR.length());

            path = path.replace(MAPPING_SEPARATOR, PATH_SEPARATOR);
            return new Identifier(namespace, path);
        }

        return Identifier.tryParse(input);
    }
}