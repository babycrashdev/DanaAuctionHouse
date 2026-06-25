package cc.synkdev.nah.manager;

import cc.synkdev.nah.NexusAuctionHouse;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import dev.triumphteam.gui.builder.item.SkullBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.components.GuiAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

    public static Component parseMiniMessage(String input) {
        if (input == null) return Component.empty();

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher hexMatcher = hexPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, "<color:#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(sb);
        String step1 = sb.toString();

        Pattern colorPattern = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
        Matcher colorMatcher = colorPattern.matcher(step1);
        sb = new StringBuffer();
        while (colorMatcher.find()) {
            char code = Character.toLowerCase(colorMatcher.group(1).charAt(0));
            String tag = switch (code) {
                case '0' -> "<black>";
                case '1' -> "<dark_blue>";
                case '2' -> "<dark_green>";
                case '3' -> "<dark_aqua>";
                case '4' -> "<dark_red>";
                case '5' -> "<dark_purple>";
                case '6' -> "<gold>";
                case '7' -> "<gray>";
                case '8' -> "<dark_gray>";
                case '9' -> "<blue>";
                case 'a' -> "<green>";
                case 'b' -> "<aqua>";
                case 'c' -> "<red>";
                case 'd' -> "<light_purple>";
                case 'e' -> "<yellow>";
                case 'f' -> "<white>";
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                case 'r' -> "<reset>";
                default -> "&" + code;
            };
            colorMatcher.appendReplacement(sb, tag);
        }
        colorMatcher.appendTail(sb);

        return MiniMessage.miniMessage().deserialize("<!italic>" + sb.toString());
    }

    public static String toLegacy(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static String getMsg(String key, String... args) {
        if (langConfig == null) return key;
        String msg = langConfig.getString(key);
        if (msg == null) {
            return key;
        }
        msg = Util.addPlaceholders(msg, args);
        return toLegacy(parseMiniMessage(msg));
    }

    public static Component getMsgComponent(String key, String... args) {
        if (langConfig == null) return Component.text(key);
        String msg = langConfig.getString(key);
        if (msg == null) {
            return Component.text(key);
        }
        msg = Util.addPlaceholders(msg, args);
        return parseMiniMessage(msg);
    }

    public static Component getGuiTitle(String menuPath, String defaultTitle) {
        if (guiConfig == null) return parseMiniMessage(defaultTitle);
        String title = guiConfig.getString("menus." + menuPath + ".title");
        if (title == null) return parseMiniMessage(defaultTitle);
        return parseMiniMessage(title);
    }

    public static int getGuiRows(String menuPath, int defaultRows) {
        if (guiConfig == null) return defaultRows;
        return guiConfig.getInt("menus." + menuPath + ".rows", defaultRows);
    }

    public static int getGuiSlot(String menuPath, String itemPath, int defaultSlot) {
        if (guiConfig == null) return defaultSlot;
        return guiConfig.getInt("menus." + menuPath + ".items." + itemPath + ".slot", defaultSlot);
    }

    public static BaseItemBuilder<?> getGuiItemBuilder(String menuPath, String itemPath, Material defaultMaterial, String defaultName, List<String> defaultLore) {
        String fullPath = "menus." + menuPath + ".items." + itemPath;
        FileConfiguration config = getGui();
        if (config == null) {
            BaseItemBuilder<?> builder = defaultMaterial == Material.PLAYER_HEAD ? ItemBuilder.skull() : ItemBuilder.from(defaultMaterial);
            if (defaultName != null) builder.name(parseMiniMessage(defaultName));
            if (defaultLore != null && !defaultLore.isEmpty()) {
                List<Component> components = new ArrayList<>();
                for (String line : defaultLore) components.add(parseMiniMessage(line));
                builder.lore(components);
            }
            return builder;
        }

        String matName = config.getString(fullPath + ".material", defaultMaterial.name());
        Material material = Material.matchMaterial(matName);
        if (material == null) material = defaultMaterial;

        BaseItemBuilder<?> builder = material == Material.PLAYER_HEAD ? ItemBuilder.skull() : ItemBuilder.from(material);

        String displayName = config.getString(fullPath + ".name", defaultName);
        if (displayName != null) {
            builder.name(parseMiniMessage(displayName));
        }

        List<String> loreList = config.getStringList(fullPath + ".lore");
        if (loreList.isEmpty() && defaultLore != null) {
            loreList = defaultLore;
        }
        if (!loreList.isEmpty()) {
            List<Component> components = new ArrayList<>();
            for (String line : loreList) {
                components.add(parseMiniMessage(line));
            }
            builder.lore(components);
        }

        builder.flags(ItemFlag.HIDE_ATTRIBUTES);
        return builder;
    }

    public static GuiItem getGuiItem(String menuPath, String itemPath, Material defaultMaterial, String defaultName, List<String> defaultLore, GuiAction<InventoryClickEvent> action) {
        BaseItemBuilder<?> builder = getGuiItemBuilder(menuPath, itemPath, defaultMaterial, defaultName, defaultLore);
        if (action != null) {
            return builder.asGuiItem(action);
        } else {
            return builder.asGuiItem();
        }
    }

    public static GuiItem getFillerItem(String menuPath) {
        String path = "menus." + menuPath + ".items.filler";
        FileConfiguration config = getGui();
        String matName = config != null ? config.getString(path + ".material", config.getString("filler", "GRAY_STAINED_GLASS_PANE")) : "GRAY_STAINED_GLASS_PANE";
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.GRAY_STAINED_GLASS_PANE;

        String name = config != null ? config.getString(path + ".name", " ") : " ";
        List<String> lore = config != null ? config.getStringList(path + ".lore") : new ArrayList<>();

        ItemBuilder builder = ItemBuilder.from(material).name(parseMiniMessage(name));
        if (!lore.isEmpty()) {
            List<Component> components = new ArrayList<>();
            for (String line : lore) {
                components.add(parseMiniMessage(line));
            }
            builder.lore(components);
        }
        return builder.asGuiItem();
    }
}
