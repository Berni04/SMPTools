package com.smp.smptools.tags;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TagManager {

    private final SMPTools plugin;
    private final Map<String, String> playerTitles = new ConcurrentHashMap<>(); // UUID -> Title

    public TagManager(SMPTools plugin) {
        this.plugin = plugin;
        loadPlayerTitles();
    }

    private void loadPlayerTitles() {
        ConfigurationSection titlesSection = plugin.getTagsConfig().getConfigurationSection("player-titles");
        if (titlesSection != null) {
            for (String uuid : titlesSection.getKeys(false)) {
                playerTitles.put(uuid, titlesSection.getString(uuid));
            }
        }
    }

    public @Nullable String getPlayerTitle(@NotNull Player player) {
        return playerTitles.get(player.getUniqueId().toString());
    }

    public void setPlayerTitle(@NotNull Player player, @NotNull String title) {
        playerTitles.put(player.getUniqueId().toString(), title);
        plugin.getTagsConfig().set("player-titles." + player.getUniqueId().toString(), title);
        plugin.saveTagsConfig();
        plugin.getNameTagListener().updatePlayerName(player); // Update display name immediately
    }

    public void removePlayerTitle(@NotNull Player player) {
        playerTitles.remove(player.getUniqueId().toString());
        plugin.getTagsConfig().set("player-titles." + player.getUniqueId().toString(), null);
        plugin.saveTagsConfig();
        plugin.getNameTagListener().updatePlayerName(player); // Update display name immediately
    }

    // Milestone logic
    public void checkMilestones(@NotNull Player player) {
        String uuid = player.getUniqueId().toString();
        ConfigurationSection stats = plugin.getStatsConfig().getConfigurationSection("stats." + uuid);
        if (stats == null) return;

        ConfigurationSection milestones = plugin.getTagsConfig().getConfigurationSection("milestones");
        if (milestones == null) return;

        for (String key : milestones.getKeys(false)) {
            ConfigurationSection milestone = milestones.getConfigurationSection(key);
            if (milestone == null) continue;

            String title = milestone.getString("title");
            String statistic = milestone.getString("statistic");
            if (title == null || statistic == null) continue;

            long requiredValue = milestone.getLong("value");
            long playerValue = stats.getLong(statistic, 0);

            if (playerValue >= requiredValue && !hasUnlockedTitle(player, title)) {
                unlockTitle(player, title);
                Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
                plugin.getServer().broadcast(formattedPlayerName.append(Component.text(" has unlocked the title: " + title, NamedTextColor.GREEN)));
            }
        }
    }

    public boolean hasUnlockedTitle(@NotNull Player player, @NotNull String title) {
        return plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString()).contains(title);
    }

    public void unlockTitle(@NotNull Player player, @NotNull String title) {
        List<String> unlockedTitles = plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString());
        if (!unlockedTitles.contains(title)) {
            unlockedTitles.add(title);
            plugin.getTagsConfig().set("unlocked-titles." + player.getUniqueId().toString(), unlockedTitles);
            plugin.saveTagsConfig();
        }
    }

    public @NotNull List<String> getUnlockedTitles(@NotNull Player player) {
        return plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString());
    }

    public @Nullable String getTagDescription(@NotNull String title) {
        if (title == null) return null;

        ConfigurationSection milestones = plugin.getTagsConfig().getConfigurationSection("milestones");
        if (milestones == null) return null;

        for (String key : milestones.getKeys(false)) {
            ConfigurationSection milestone = milestones.getConfigurationSection(key);
            if (milestone == null) continue;

            String milestoneTitle = milestone.getString("title");
            if (title.equals(milestoneTitle)) {
                return milestone.getString("description");
            }
        }
        return null;
    }
}
