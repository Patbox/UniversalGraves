package eu.pb4.graves.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.graves.config.data.VersionedConfigData;
import eu.pb4.graves.config.data.old.ConfigDataV1;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConfigManager {
    public static final int VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setLenient().create();

    private static Config CONFIG = new Config(new ConfigData());
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
            ConfigData config;
            File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "universal-graves.json");


            if (configFile.exists()) {
                String json = IOUtils.toString(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

                config = switch (GSON.fromJson(json, VersionedConfigData.class).CONFIG_VERSION_DONT_TOUCH_THIS) {
                    case 1 -> GSON.fromJson(json, ConfigDataV1.class).update();
                    default -> GSON.fromJson(json, ConfigData.class);
                };

                config.CONFIG_VERSION_DONT_TOUCH_THIS = VERSION;
            } else {
                config = new ConfigData();
            }

            overrideConfig(config);
            ENABLED = true;
        }
        catch(IOException exception) {
            ENABLED = false;
            GravesMod.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
        }

        return ENABLED;
    }

    public static void overrideConfig(ConfigData configData) {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "universal-graves.json");
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8));
            writer.write(GSON.toJson(configData));
            writer.close();
            CONFIG = new Config(configData);
        } catch (Exception e) {
            GravesMod.LOGGER.error("Something went wrong while saving config!");
            e.printStackTrace();
        }
    }
}
