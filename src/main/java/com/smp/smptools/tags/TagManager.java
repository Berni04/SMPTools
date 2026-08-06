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

/**
 * Manages player titles/tags and milestone-based unlocks.
 * Handles equipping, unequipping, and checking milestone progress for titles.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Thread-safe title storage using ConcurrentHashMap</li>
 *   <li>Milestone-based title unlocking</li>
 *   <li>Persistent title storage in tags.yml</li>
 *   <li>Real-time display name updates</li>
 * </ul>
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public class TagManager {

    private final SMPTools plugin;
    /** Map of player UUIDs to their equipped titles */
    private final Map<String, String> playerTitles = new ConcurrentHashMap<>();

    /**
     * Constructs a new TagManager and loads saved player titles.
     *
     * @param plugin the SMPTools plugin instance
     */
    public TagManager(SMPTools plugin) {
        this.plugin = plugin;
        loadPlayerTitles();
    }

    /**
     * Loads player titles from the configuration file or storage provider.
     */
    private void loadPlayerTitles() {
        if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
            try {
                Map<String, String> titles = plugin.getStorageManager().getProvider().getAllPlayerTitles();
                if (titles != null) {
                    playerTitles.putAll(titles);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load player titles from storage provider: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the equipped title for a player by UUID.
     *
     * @param uuid the player's UUID
     * @return the player's equipped title, or null if none is equipped
     */
    public @Nullable String getPlayerTitle(@NotNull UUID uuid) {
        return playerTitles.get(uuid.toString());
    }

    /**
     * Gets the equipped title for a player.
     *
     * @param player the player to get the title for
     * @return the player's equipped title, or null if none is equipped
     */
    public @Nullable String getPlayerTitle(@NotNull Player player) {
        return getPlayerTitle(player.getUniqueId());
    }

    /**
     * Sets a player's equipped title and updates their display name.
     *
     * @param player the player to set the title for
     * @param title the title to equip
     */
    public void setPlayerTitle(@NotNull Player player, @NotNull String title) {
        playerTitles.put(player.getUniqueId().toString(), title);
        if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
            plugin.getStorageManager().getProvider().savePlayerTitle(player.getUniqueId(), title);
        }
        plugin.getNameTagListener().updatePlayerName(player); // Update display name immediately
    }

    /**
     * Removes a player's equipped title and updates their display name.
     *
     * @param player the player to remove the title from
     */
    public void removePlayerTitle(@NotNull Player player) {
        playerTitles.remove(player.getUniqueId().toString());
        if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
            plugin.getStorageManager().getProvider().removePlayerTitle(player.getUniqueId());
        }
        plugin.getNameTagListener().updatePlayerName(player); // Update display name immediately
    }

    /**
     * Checks if a player has achieved any new milestones and unlocks titles accordingly.
     * Broadcasts a message when a new title is unlocked.
     *
     * @param player the player to check milestones for
     */
    public void checkMilestones(@NotNull Player player) {
        String uuid = player.getUniqueId().toString();
        ConfigurationSection stats = plugin.getStatsConfig().getConfigurationSection("stats." + uuid);

        ConfigurationSection milestones = plugin.getTagsConfig().getConfigurationSection("milestones");
        if (milestones == null) return;

        for (String key : milestones.getKeys(false)) {
            ConfigurationSection milestone = milestones.getConfigurationSection(key);
            if (milestone == null) continue;

            String title = milestone.getString("title");
            String statistic = milestone.getString("statistic");
            if (title == null || statistic == null) continue;

            long requiredValue = milestone.getLong("value");
            long playerValue = stats != null ? stats.getLong(statistic, 0) : 0;
            if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
                long dbVal = plugin.getStorageManager().getProvider().getLongStat(player.getUniqueId(), statistic, 0);
                if (dbVal > playerValue) playerValue = dbVal;
            }

            if (playerValue >= requiredValue && !hasUnlockedTitle(player, title)) {
                unlockTitle(player, title);
                Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
                plugin.getServer().broadcast(formattedPlayerName.append(Component.text(" has unlocked the title: " + title, NamedTextColor.GREEN)));
            }
        }
    }

    /**
     * Checks if a player has unlocked a specific title.
     *
     * @param player the player to check
     * @param title the title to check for
     * @return true if the player has unlocked the title, false otherwise
     */
    public boolean hasUnlockedTitle(@NotNull Player player, @NotNull String title) {
        return plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString()).contains(title);
    }

    /**
     * Unlocks a title for a player if they don't already have it.
     *
     * @param player the player to unlock the title for
     * @param title the title to unlock
     */
    public void unlockTitle(@NotNull Player player, @NotNull String title) {
        List<String> unlockedTitles = plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString());
        if (!unlockedTitles.contains(title)) {
            unlockedTitles.add(title);
            plugin.getTagsConfig().set("unlocked-titles." + player.getUniqueId().toString(), unlockedTitles);
            plugin.saveTagsConfig();
        }
    }

    /**
     * Gets all unlocked titles for a player.
     *
     * @param player the player to get titles for
     * @return a list of unlocked title names
     */
    public @NotNull List<String> getUnlockedTitles(@NotNull Player player) {
        return plugin.getTagsConfig().getStringList("unlocked-titles." + player.getUniqueId().toString());
    }

    /**
     * Gets the description for a specific milestone title.
     *
     * @param title the title to get the description for
     * @return the description, or null if not found
     */
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
