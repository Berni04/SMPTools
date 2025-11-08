package com.smp.smptools.tags;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TagManager {

    private final SMPTools plugin;
    private final Map<String, String> playerTitles = new HashMap<>(); // UUID -> Title

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

    public String getPlayerTitle(Player player) {
        return playerTitles.get(player.getUniqueId().toString());
    }

    public void setPlayerTitle(Player player, String title) {
        playerTitles.put(player.getUniqueId().toString(), title);
        plugin.getTagsConfig().set("player-titles." + player.getUniqueId().toString(), title);
        plugin.saveTagsConfig();
        plugin.getNameTagListener().updatePlayerName(player); // Update display name immediately
    }

    public void removePlayerTitle(Player player) {
        playerTitles.remove(player.getUniqueId().toString());
        plugin.getTagsConfig().set("player-titles." + player.getUniqueId().toString(), null);
        plugin.saveTagsConfig();
        plugin.getNameTagListener().updatePlayerName(player); // Update display name immediately
    }

    // Milestone logic
    public void checkMilestones(Player player) {
        String uuid = player.getUniqueId().toString();
        ConfigurationSection stats = plugin.getStatsConfig().getConfigurationSection("stats." + uuid);
        if (stats == null) return;

        ConfigurationSection milestones = plugin.getTagsConfig().getConfigurationSection("milestones");
        if (milestones == null) return;

        for (String key : milestones.getKeys(false)) {
            ConfigurationSection milestone = milestones.getConfigurationSection(key);
            String title = milestone.getString("title");
            String statistic = milestone.getString("statistic");
            long requiredValue = milestone.getLong("value");
            long playerValue = stats.getLong(statistic, 0);

            if (playerValue >= requiredValue && !hasUnlockedTitle(player, title)) {
                unlockTitle(player, title);
                player.sendMessage("§aYou have unlocked the title: §e" + title);
            }
        }
    }

    public boolean hasUnlockedTitle(Player player, String title) {
        return plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString()).contains(title);
    }

    public void unlockTitle(Player player, String title) {
        List<String> unlockedTitles = plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString());
        if (!unlockedTitles.contains(title)) {
            unlockedTitles.add(title);
            plugin.getTagsConfig().set("unlocked-titles." + player.getUniqueId().toString(), unlockedTitles);
            plugin.saveTagsConfig();
        }
    }

    public List<String> getUnlockedTitles(Player player) {
        return plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString());
    }
}
