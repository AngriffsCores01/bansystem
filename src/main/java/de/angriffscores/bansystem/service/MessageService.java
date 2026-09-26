package de.angriffscores.bansystem.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

/**
 * @author AngriffsCores
 * @since 26.09.2026
 */
public class MessageService {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final @NonNull JavaPlugin plugin;
    private @NonNull String language;
    private @NonNull Map<String, String> messages;

    public MessageService(@NonNull JavaPlugin plugin, @NonNull String language) {
        this.plugin = plugin;
        this.language = language;
        this.messages = new HashMap<>();
        this.reload();
    }

    /**
     * Reloads messages from messages.yml.
     */
    public void reload() {
        File file = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.getConfigurationSection(this.language);
        if (section == null) {
            section = configuration.getConfigurationSection("en");
        }
        Map<String, String> loaded = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                loaded.put(key, section.getString(key, ""));
            }
        }
        this.messages = loaded;
    }

    /**
     * @param language language code
     */
    public void setLanguage(@NonNull String language) {
        this.language = language;
        this.reload();
    }

    /**
     * @param key message key
     * @return raw MiniMessage string without placeholders applied
     */
    public @NonNull String rawString(@NonNull String key) {
        return this.messages.getOrDefault(key, key);
    }

    /**
     * @param key message key
     * @param placeholders placeholder replacements
     * @return adventure component
     */
    public @NonNull Component component(@NonNull String key, @NonNull String... placeholders) {
        String withPrefix = this.applyPlaceholders(this.rawString(key), placeholders);
        return MINI_MESSAGE.deserialize(withPrefix);
    }

    private @NonNull String applyPlaceholders(@NonNull String input, @NonNull String... placeholders) {
        String result = input;
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            result = result.replace(placeholders[index], placeholders[index + 1]);
        }
        return result;
    }

    /**
     * @param key message key
     * @param placeholders placeholder replacements
     * @return adventure component including configured prefix
     */
    public @NonNull Component prefixed(@NonNull String key, @NonNull String... placeholders) {
        String prefix = this.messages.getOrDefault("prefix", "");
        String combined = this.applyPlaceholders(prefix + this.rawString(key), placeholders);
        return MINI_MESSAGE.deserialize(combined);
    }

    /**
     * Sends a prefixed message to a sender.
     *
     * @param sender command sender
     * @param key message key
     * @param placeholders placeholder replacements
     */
    public void send(@NonNull CommandSender sender, @NonNull String key, @NonNull String... placeholders) {
        sender.sendMessage(this.prefixed(key, placeholders));
    }

    /**
     * @param key message key
     * @param placeholders placeholder replacements
     * @return plain-ish serialized message without prefix for kick screens etc.
     */
    public @NonNull Component raw(@NonNull String key, @NonNull String... placeholders) {
        return this.component(key, placeholders);
    }
}
