package eu.pb4.graves.config;

import com.google.gson.JsonParser;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.data.LegacyConfigData;
import eu.pb4.graves.model.GraveModel;
import eu.pb4.graves.other.ImplementedInventory;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static final int VERSION = 3;
    private static final Path BASE_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("universal-graves/");
    private static final Path CONFIG_PATH = BASE_CONFIG_PATH.resolve("config.json");
    private static final Path MODELS_PATH = BASE_CONFIG_PATH.resolve("models/");
    private static final Path OLD_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("universal-graves.json");
    private static final Map<String, GraveModel> MODELS = new HashMap<>();

    private static Config CONFIG = new Config();
    private static boolean ENABLED = false;

    public static Config getConfig() {
        if (CONFIG == null) {
            loadConfig();
        }
        return CONFIG;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static boolean loadConfig() {
        ENABLED = false;

        CONFIG = null;
        try {
            Config config;
            MODELS.clear();
            if (Files.exists(MODELS_PATH)) {
                Files.newDirectoryStream(MODELS_PATH).forEach((path) -> {
                    try {
                        var name = MODELS_PATH.relativize(path).toString();
                        MODELS.put(name.substring(0, name.length() - 5), BaseGson.GSON.fromJson(Files.readString(path), GraveModel.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                Files.createDirectories(MODELS_PATH);
                Files.writeString(MODELS_PATH.resolve("example.json"), BaseGson.GSON.toJson(GraveModel.DEFAULT_MODEL));
                MODELS.put("example", GraveModel.DEFAULT_MODEL.get());
            }


            if (Files.exists(CONFIG_PATH)) {
                config = BaseGson.GSON.fromJson(Files.readString(CONFIG_PATH), Config.class);
            } else if (Files.exists(OLD_CONFIG_PATH)) {
                config = BaseGson.GSON.fromJson(Files.readString(OLD_CONFIG_PATH), LegacyConfigData.class).convert();
            } else {
                config = new Config();
            }
            config.fillMissing();
            overrideConfig(config);
            CONFIG = config;
            ENABLED = true;
        }
            catch(Throwable exception) {
            ENABLED = false;
            GravesMod.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
        }

        return ENABLED;
    }

    public static void overrideConfig(Config configData) {
        try {
            Files.writeString(CONFIG_PATH, BaseGson.GSON.toJson(configData));
            CONFIG = configData;
        } catch (Exception e) {
            GravesMod.LOGGER.error("Something went wrong while saving config!");
            e.printStackTrace();
        }
    }

    public static GraveModel getModel(String model, GraveModel defaultModel) {
        return MODELS.getOrDefault(model, defaultModel);
    }
}
