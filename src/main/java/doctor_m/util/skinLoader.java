package doctor_m.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class skinLoader {
    private static List<String> skinNames = null;

    public static List<String> getAvailableSkins() {
        if (skinNames != null) return skinNames;
        skinNames = new ArrayList<>();
        // 由于 Fabric 无法直接列举资源包中的文件，需要通过访问 assets 目录下的一个“索引文件”来获取。
        // 推荐做法：用一个 JSON 文件列出所有皮肤名。
        // 这里假设你已经手动在资源包中维护了一个 list.txt 或 skins.json。
        // 为了简单，我们直接在代码中硬编码列表，或者通过读取 assets/doctor_m/textures/entity/tardis_skins/skins.txt。
        try {
            // 方式一：读取 skins.txt（每行一个皮肤名，不含扩展名）
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    skinLoader.class.getResourceAsStream("/assets/doctor_m/textures/entity/tardis_skins/skins.txt")
            ));
            skinNames = reader.lines().collect(Collectors.toList());
            reader.close();
        } catch (Exception e) {
            // 如果文件不存在，返回一个默认列表
            skinNames.add("alice");
            skinNames.add("bob");
        }
        return skinNames;
    }
}