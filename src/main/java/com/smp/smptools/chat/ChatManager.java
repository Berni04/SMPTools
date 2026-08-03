package com.smp.smptools.chat;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.InputValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {

    private final SMPTools plugin;
    private final Map<UUID, UUID> lastMessengers = new ConcurrentHashMap<>(); // recipient UUID -> sender UUID

    public ChatManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void setLastMessenger(UUID recipient, UUID sender) {
        lastMessengers.put(recipient, sender);
    }

    public UUID getLastMessenger(UUID recipient) {
        return lastMessengers.get(recipient);
    }

    public Component getFormattedDisplayName(Player player) {
        FileConfiguration statsConfig = plugin.getStatsConfig();
        FileConfiguration tagsConfig = plugin.getTagsConfig();
        String playerUUID = player.getUniqueId().toString();

        // 1. Get the raw color tag (e.g., "<red>" or "<#ff0000>"), defaulting to "<white>"
        // Sanitize to prevent MiniMessage tag injection (hover/click/etc.) from saved values.
        String colorTag = InputValidator.sanitizeMiniMessage(
                statsConfig.getString("players." + playerUUID + ".name-color", "<white>"));
        if (colorTag.isEmpty()) colorTag = "<white>";

        // 2. Get prefix and tag strings
        String prefixStr = InputValidator.sanitizeMiniMessage(
                statsConfig.getString("players." + playerUUID + ".prefix", ""));
        String tagTitle = tagsConfig.getString("player-titles." + playerUUID);

        // 3. Build a single MiniMessage string
        StringBuilder mmString = new StringBuilder();

        mmString.append(colorTag);

        if (!prefixStr.isEmpty()) {
            mmString.append(prefixStr).append(" ");
        }

        mmString.append(player.getName());

        if (tagTitle != null && !tagTitle.isEmpty()) {
            String tagDescription = plugin.getTagManager().getTagDescription(tagTitle);
            if (tagDescription != null && !tagDescription.isEmpty()) {
                String sanitizedDescription = InputValidator.sanitizeMiniMessage(tagDescription)
                        .replace("'", "\\'");
                mmString.append(" <hover:show_text:'").append(colorTag).append(sanitizedDescription).append("'>[").append(tagTitle).append("]</hover>");
            } else {
                mmString.append(" [").append(tagTitle).append("]");
            }
        }

        return MiniMessage.miniMessage().deserialize(mmString.toString());
    }
}
