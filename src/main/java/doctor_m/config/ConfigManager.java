package doctor_m.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "doctor_m.json"; // 你的模组ID
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig config;

    public static void loadConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        File configFile = configPath.toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                config = new ModConfig(); // 加载失败则使用默认值
            }
        } else {
            // 文件不存在，使用默认值并保存
            config = new ModConfig();
            saveConfig();
        }
    }

    public static void saveConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        try (Writer writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ModConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
}