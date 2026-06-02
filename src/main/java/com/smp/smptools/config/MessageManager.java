package com.smp.smptools.config;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Manages externalized messages with MiniMessage support.
 * Provides dynamic player tags for personalized messages.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public class MessageManager {

    private final SMPTools plugin;
    private FileConfiguration messagesConfig;

    /**
     * Constructs a new MessageManager and loads messages.yml.
     *
     * @param plugin the SMPTools plugin instance
     */
    public MessageManager(SMPTools plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Loads the messages.yml configuration file.
     */
    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Gets a message from the configuration with player context and placeholders.
     *
     * @param path the message path in messages.yml
     * @param player the player context (can be null)
     * @param placeholders the placeholder values to replace
     * @return the formatted Component message
     */
    public Component getMessage(String path, Player player, Map<String, String> placeholders) {
        String msg = messagesConfig.getString(path, "Missing message: " + path);

        // Replace simple {placeholder} values
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        // Build dynamic tag resolvers
        TagResolver resolver = buildPlayerTagResolver(player);

        return MiniMessage.miniMessage().deserialize(msg, resolver);
    }

    /**
     * Gets a message from the configuration with player context.
     *
     * @param path the message path in messages.yml
     * @param player the player context (can be null)
     * @return the formatted Component message
     */
    public Component getMessage(String path, Player player) {
        return getMessage(path, player, Collections.emptyMap());
    }

    /**
     * Gets a message from the configuration without player context.
     *
     * @param path the message path in messages.yml
     * @return the formatted Component message
     */
    public Component getMessage(String path) {
        return getMessage(path, null, Collections.emptyMap());
    }

    /**
     * Gets the raw string message from the configuration without parsing.
     * Useful when the caller wants to deserialize via a different serializer
     * (e.g. {@code MiniMessage.deserialize(...)} or for lore/displayName fields).
     *
     * @param path the message path in messages.yml
     * @return the raw MiniMessage template string, or empty string if not found
     */
    public String getRawMessage(String path) {
        String msg = messagesConfig.getString(path, "");
        return msg == null ? "" : msg;
    }

    /**
     * Gets a string list from the messages configuration.
     *
     * @param path the message path in messages.yml
     * @return the list of strings, or empty list if not found
     */
    public java.util.List<String> getStringList(String path) {
        java.util.List<String> list = messagesConfig.getStringList(path);
        return list == null ? java.util.Collections.emptyList() : list;
    }

    /**
     * Builds a TagResolver with all player-related dynamic tags.
     *
     * @param player the player to build tags for
     * @return the TagResolver with player tags
     */
    private TagResolver buildPlayerTagResolver(Player player) {
        if (player == null) {
            return TagResolver.empty();
        }

        FileConfiguration statsConfig = plugin.getStatsConfig();
        FileConfiguration tagsConfig = plugin.getTagsConfig();
        String uuid = player.getUniqueId().toString();

        // Get player data
        String colorTag = statsConfig.getString("players." + uuid + ".name-color", "<white>");
        String prefix = statsConfig.getString("players." + uuid + ".prefix", "");
        String title = tagsConfig.getString("player-titles." + uuid, "");

        // Full formatted name (color + prefix + name + title)
        Component fullPlayer = plugin.getChatManager().getFormattedDisplayName(player);

        // Name with color only (no prefix/title)
        Component nameColor = MiniMessage.miniMessage().deserialize(colorTag + player.getName());

        // Just the raw name
        Component rawName = Component.text(player.getName());

        // Just the color
        Component color = MiniMessage.miniMessage().deserialize(colorTag);

        // Just the prefix
        Component prefixComponent = prefix.isEmpty() 
            ? Component.empty() 
            : MiniMessage.miniMessage().deserialize(colorTag + prefix);

        // Just the title
        Component titleComponent = title.isEmpty() 
            ? Component.empty() 
            : Component.text("[" + title + "]");

        return TagResolver.builder()
            .resolver(TagResolver.resolver("player", Tag.selfClosingInserting(fullPlayer)))
            .resolver(TagResolver.resolver("player_name", Tag.selfClosingInserting(rawName)))
            .resolver(TagResolver.resolver("player_name_color", Tag.selfClosingInserting(nameColor)))
            .resolver(TagResolver.resolver("player_color", Tag.selfClosingInserting(color)))
            .resolver(TagResolver.resolver("player_prefix", Tag.selfClosingInserting(prefixComponent)))
            .resolver(TagResolver.resolver("player_title", Tag.selfClosingInserting(titleComponent)))
            .build();
    }
}
