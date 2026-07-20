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
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    skinLoader.class.getResourceAsStream("/assets/doctor_m/textures/entity/tardis/skins.txt")
            ));
            skinNames = reader.lines().collect(Collectors.toList());
            reader.close();
        } catch (Exception e) {
            skinNames.add("alice");
            skinNames.add("bob");
        }
        return skinNames;
    }
}