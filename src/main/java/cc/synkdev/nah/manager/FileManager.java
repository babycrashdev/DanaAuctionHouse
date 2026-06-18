package cc.synkdev.nah.manager;

import cc.synkdev.nah.NexusAuctionHouse;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class FileManager {
    private static final NexusAuctionHouse core = NexusAuctionHouse.getInstance();
    private static File langFile;
    private static File guiFile;
    private static FileConfiguration langConfig;
    private static FileConfiguration guiConfig;

    public static void init() {
        if (!core.getDataFolder().exists()) {
            core.getDataFolder().mkdirs();
        }

        langFile = new File(core.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            core.saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        guiFile = new File(core.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            core.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    public static void reload() {
        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        }
        if (guiFile.exists()) {
            guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        }
    }

    public static FileConfiguration getLang() {
        return langConfig;
    }

    public static FileConfiguration getGui() {
        return guiConfig;
    }

    public static String getMsg(String key, String... args) {
        if (langConfig == null) return key;
        String msg = langConfig.getString(key);
        if (msg == null) {
            return key;
        }
        msg = Util.color(msg);
        return Util.addPlaceholders(msg, args);
    }

    public static Material getGuiMaterial(String key, Material def) {
        if (guiConfig == null) return def;
        String matName = guiConfig.getString(key);
        if (matName == null) return def;
        try {
            return Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
