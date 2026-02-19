package me.son14ka.mineChess;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageService {
    private final MineChess plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private FileConfiguration enConfig;
    private FileConfiguration ukConfig;
    private FileConfiguration zhConfig;

    public MessageService(MineChess plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        enConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages_en.yml"));
        ukConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages_uk.yml"));
        zhConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages_zh.yml"));
    }

    public Component msg(Player player, String path, TagResolver... placeholders) {
        String rawLocale = player.getLocale().toLowerCase();
        String locale;
        if (rawLocale.startsWith("uk")) locale = "uk";
        else if (rawLocale.startsWith("zh")) locale = "zh";
        else locale = "en";

        FileConfiguration config = switch (locale) {
            case "uk" -> ukConfig;
            case "zh" -> zhConfig;
            default -> enConfig;
        };
        String raw = config.getString("messages." + path, "<red>Missing key: " + path);
        return mini.deserialize(raw, placeholders);
    }
}
