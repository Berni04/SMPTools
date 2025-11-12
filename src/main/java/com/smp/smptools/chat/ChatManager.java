package com.smp.smptools.chat;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ChatManager {

    private final SMPTools plugin;

    public ChatManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public Component getFormattedDisplayName(Player player) {
        FileConfiguration statsConfig = plugin.getStatsConfig();
        FileConfiguration tagsConfig = plugin.getTagsConfig();
        String playerUUID = player.getUniqueId().toString();

        // 1. Get the raw color tag (e.g., "<red>" or "<#ff0000>"), defaulting to "<white>"
        String colorTag = statsConfig.getString("players." + playerUUID + ".name-color", "<white>");

        // 2. Get prefix and tag strings
        String prefixStr = statsConfig.getString("players." + playerUUID + ".prefix", "");
        String tagTitle = tagsConfig.getString("player-titles." + playerUUID);

        // 3. Build a single MiniMessage string
        StringBuilder mmString = new StringBuilder();
        
        // The color tag will apply to everything that follows it.
        mmString.append(colorTag);

        if (!prefixStr.isEmpty()) {
            mmString.append(prefixStr).append(" ");
        }

        mmString.append(player.getName());

        if (tagTitle != null && !tagTitle.isEmpty()) {
            String tagDescription = plugin.getTagManager().getTagDescription(tagTitle);
            if (tagDescription != null && !tagDescription.isEmpty()) {
                // Construct the hover text content, ensuring it's also colored
                // The description itself might contain MiniMessage, so we just pass it.
                // We need to escape single quotes within the description for the hover tag.
                String escapedDescription = tagDescription.replace("'", "\\'");

                // Append the hoverable tag: <hover:show_text:'<color>description'>[TagTitle]</hover>
                mmString.append(" <hover:show_text:'").append(colorTag).append(escapedDescription).append("'>[").append(tagTitle).append("]</hover>");
            } else {
                // If no description, just append the tag
                mmString.append(" [").append(tagTitle).append("]");
            }
        }

        // 4. Deserialize the complete string once
        return MiniMessage.miniMessage().deserialize(mmString.toString());
    }
}
