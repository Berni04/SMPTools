package com.smp.smptools.tags;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class TagManager implements Listener {

    private final SMPTools plugin;
    /** Map of player UUIDs to their equipped titles */
    private final Map<String, String> playerTitles = new ConcurrentHashMap<>();
    /** In-memory cache of player milestone stats loaded asynchronously */
    private final Map<UUID, Map<String, Long>> milestoneStatCache = new ConcurrentHashMap<>();
    private final Set<UUID> loadingPlayers = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.atomic.AtomicInteger globalCacheVersion = new java.util.concurrent.atomic.AtomicInteger(0);
    private final Map<UUID, java.util.concurrent.atomic.AtomicInteger> playerCacheVersions = new ConcurrentHashMap<>();

    /**
     * Constructs a new TagManager and loads saved player titles.
     *
     * @param plugin the SMPTools plugin instance
     */
    public TagManager(SMPTools plugin) {
        this.plugin = plugin;
        loadPlayerTitlesAsync();
        if (plugin != null && Bukkit.getServer() != null && plugin.getServer() != null) {
            try {
                Bukkit.getPluginManager().registerEvents(this, plugin);
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        evictPlayerCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        evictPlayerCache(event.getPlayer().getUniqueId());
    }

    void evictPlayerCache(@NotNull UUID uuid) {
        playerCacheVersions.computeIfPresent(uuid, (k, v) -> { v.incrementAndGet(); return v; });
        milestoneStatCache.remove(uuid);
        if (!loadingPlayers.contains(uuid)) {
            playerCacheVersions.remove(uuid);
        }
    }

    /**
     * Triggers asynchronous title loading during plugin enable.
     */
    public void loadPlayerTitlesAsync() {
        if (plugin != null && Bukkit.getServer() != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::loadPlayerTitles);
        } else {
            loadPlayerTitles();
        }
    }

    /**
     * Loads player titles from the storage provider.
     * Also migrates legacy tags.yml titles into the selected provider ONLY when title storage is empty.
     */
    public void loadPlayerTitles() {
        if (plugin != null && plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
            try {
                Map<String, String> titles = plugin.getStorageManager().getProvider().getAllPlayerTitles();

                // Perform legacy title migration ONLY when title storage is empty
                if (titles == null || titles.isEmpty()) {
                    Map<String, String> legacySnapshot = snapshotLegacyTitlesSync();
                    if (!legacySnapshot.isEmpty()) {
                        for (Map.Entry<String, String> entry : legacySnapshot.entrySet()) {
                            try {
                                UUID uuid = UUID.fromString(entry.getKey());
                                plugin.getStorageManager().getProvider().savePlayerTitle(uuid, entry.getValue());
                                playerTitles.put(entry.getKey(), entry.getValue());
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                    titles = plugin.getStorageManager().getProvider().getAllPlayerTitles();
                }

                if (titles != null) {
                    playerTitles.putAll(titles);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load player titles from storage provider: " + e.getMessage());
            }
        }
    }

    private Map<String, String> snapshotLegacyTitlesSync() {
        if (plugin == null) {
            return java.util.Collections.emptyMap();
        }
        if (Bukkit.getServer() == null || Bukkit.isPrimaryThread()) {
            return readLegacyTitlesFromConfig();
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin, this::readLegacyTitlesFromConfig).get();
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }

    private Map<String, String> readLegacyTitlesFromConfig() {
        if (plugin == null || plugin.getTagsConfig() == null) {
            return java.util.Collections.emptyMap();
        }
        ConfigurationSection legacySection = plugin.getTagsConfig().getConfigurationSection("player-titles");
        if (legacySection == null) {
            legacySection = plugin.getTagsConfig().getConfigurationSection("tags");
        }
        if (legacySection == null) {
            return java.util.Collections.emptyMap();
        }
        Map<String, String> legacyMap = new java.util.HashMap<>();
        for (String key : legacySection.getKeys(false)) {
            String legacyTitle = legacySection.getString(key);
            if (legacyTitle != null && !legacyTitle.isEmpty()) {
                legacyMap.put(key, legacyTitle);
            }
        }
        return legacyMap;
    }

    /**
     * Loads a player's statistics asynchronously from the storage provider into the in-memory cache.
     *
     * @param uuid the player's UUID
     */
    public void loadPlayerStatsAsync(@NotNull UUID uuid) {
        if (plugin == null || plugin.getStorageManager() == null || plugin.getStorageManager().getProvider() == null) return;
        if (!loadingPlayers.add(uuid)) return; // Already loading

        int globalVer = globalCacheVersion.get();
        int playerVer = playerCacheVersions.computeIfAbsent(uuid, k -> new java.util.concurrent.atomic.AtomicInteger(0)).get();

        boolean isFlatFile = plugin.getStorageManager().getProvider() instanceof com.smp.smptools.storage.FlatFileStorageProvider;
        Map<String, Object> flatFileSnapshotTemp = null;
        if (isFlatFile) {
            if (Bukkit.getServer() == null || Bukkit.isPrimaryThread()) {
                try {
                    flatFileSnapshotTemp = plugin.getStorageManager().getProvider().getAllPlayerStats(uuid);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load stats for " + uuid + ": " + e.getMessage());
                    loadingPlayers.remove(uuid);
                    return;
                }
            } else {
                try {
                    flatFileSnapshotTemp = Bukkit.getScheduler().callSyncMethod(plugin, () ->
                            plugin.getStorageManager().getProvider().getAllPlayerStats(uuid)
                    ).get();
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load stats for " + uuid + ": " + e.getMessage());
                    loadingPlayers.remove(uuid);
                    return;
                }
            }
        }
        final Map<String, Object> flatFileSnapshot = flatFileSnapshotTemp;

        Runnable asyncTask = () -> {
            Map<String, Long> parsedStats = null;
            boolean readFailed = false;
            try {
                Map<String, Object> allStats = isFlatFile ? flatFileSnapshot : plugin.getStorageManager().getProvider().getAllPlayerStats(uuid);
                if (allStats == null) {
                    readFailed = true;
                } else {
                    parsedStats = new ConcurrentHashMap<>();
                    for (Map.Entry<String, Object> entry : allStats.entrySet()) {
                        if (entry.getValue() != null) {
                            try {
                                parsedStats.put(entry.getKey(), Long.parseLong(entry.getValue().toString()));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load stats asynchronously for " + uuid + ": " + e.getMessage());
                readFailed = true;
            }

            final Map<String, Long> finalParsedStats = parsedStats;
            final boolean finalReadFailed = readFailed;

            Runnable mainThreadPublishTask = () -> {
                try {
                    if (plugin == null || !plugin.isEnabled()) {
                        return; // Plugin disabled; perform thread-safe cleanup without running Bukkit/milestone logic
                    }
                    if (!finalReadFailed && finalParsedStats != null) {
                        int currentGlobalVer = globalCacheVersion.get();
                        int currentPlayerVer = playerCacheVersions.computeIfAbsent(uuid, k -> new java.util.concurrent.atomic.AtomicInteger(0)).get();
                        Player player = Bukkit.getServer() != null ? Bukkit.getPlayer(uuid) : null;
                        boolean isOnline = Bukkit.getServer() == null || player != null;
                        if (isOnline && globalVer == currentGlobalVer && playerVer == currentPlayerVer) {
                            milestoneStatCache.put(uuid, finalParsedStats);
                            if (player != null && player.isOnline()) {
                                checkMilestones(player);
                            }
                        }
                    }
                } finally {
                    loadingPlayers.remove(uuid);
                    if (plugin == null || !plugin.isEnabled()) {
                        playerCacheVersions.remove(uuid);
                    } else if (Bukkit.getServer() != null && Bukkit.getPlayer(uuid) == null) {
                        playerCacheVersions.remove(uuid);
                    }
                }
            };

            if (Bukkit.getServer() == null || !plugin.isEnabled()) {
                mainThreadPublishTask.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, mainThreadPublishTask);
            }
        };

        if (Bukkit.getServer() == null || !plugin.isEnabled()) {
            asyncTask.run();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, asyncTask);
        }
    }

    /**
     * Clears cached milestone stats for a specific player (or all players if uuid is null).
     *
     * @param uuid the player's UUID
     */
    public void clearCachedStats(@Nullable UUID uuid) {
        if (uuid != null) {
            playerCacheVersions.computeIfAbsent(uuid, k -> new java.util.concurrent.atomic.AtomicInteger(0)).incrementAndGet();
            milestoneStatCache.remove(uuid);
            loadingPlayers.remove(uuid);
        } else {
            globalCacheVersion.incrementAndGet();
            milestoneStatCache.clear();
            loadingPlayers.clear();
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

    public @Nullable String getTagTitle(@NotNull UUID uuid) {
        return getPlayerTitle(uuid);
    }

    public @Nullable String getTagTitle(@NotNull Player player) {
        return getPlayerTitle(player);
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
     * Uses in-memory cached stats and async loading to prevent blocking the main thread.
     *
     * @param player the player to check milestones for
     */
    public void checkMilestones(@NotNull Player player) {
        UUID playerUuid = player.getUniqueId();
        String uuid = playerUuid.toString();
        ConfigurationSection stats = plugin.getStatsConfig().getConfigurationSection("stats." + uuid);

        ConfigurationSection milestones = plugin.getTagsConfig().getConfigurationSection("milestones");
        if (milestones == null) return;

        Map<String, Long> userCache = milestoneStatCache.get(playerUuid);
        if (userCache == null && plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
            loadPlayerStatsAsync(playerUuid);
        }

        for (String key : milestones.getKeys(false)) {
            ConfigurationSection milestone = milestones.getConfigurationSection(key);
            if (milestone == null) continue;

            String title = milestone.getString("title");
            String statistic = milestone.getString("statistic");
            if (title == null || statistic == null) continue;

            long requiredValue = milestone.getLong("value");
            long playerValue = getStatFromConfig(stats, statistic);

            if (userCache != null) {
                Long cachedDbVal = getStatFromCache(userCache, statistic);
                if (cachedDbVal != null && cachedDbVal > playerValue) {
                    playerValue = cachedDbVal;
                }
            }

            if (playerValue >= requiredValue && !hasUnlockedTitle(player, title)) {
                unlockTitle(player, title);
                Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
                plugin.getServer().broadcast(formattedPlayerName.append(Component.text(" has unlocked the title: " + title, NamedTextColor.GREEN)));
            }
        }
    }

    private long getStatFromConfig(ConfigurationSection stats, String statistic) {
        if (stats == null || statistic == null) return 0;
        if (stats.contains(statistic)) {
            return stats.getLong(statistic);
        }
        String dotVersion = statistic.replace('_', '.');
        if (stats.contains(dotVersion)) {
            return stats.getLong(dotVersion);
        }
        String underscoreVersion = statistic.replace('.', '_');
        if (stats.contains(underscoreVersion)) {
            return stats.getLong(underscoreVersion);
        }
        return 0;
    }

    Long getStatFromCache(Map<String, Long> userCache, String statistic) {
        if (userCache == null || statistic == null) return null;
        Long val = userCache.get(statistic);
        if (val != null) return val;
        val = userCache.get(statistic.replace('.', '_'));
        if (val != null) return val;
        val = userCache.get(statistic.replace('_', '.'));
        if (val != null) return val;
        int lastUnderscore = statistic.lastIndexOf('_');
        if (lastUnderscore >= 0) {
            String candidate = statistic.substring(0, lastUnderscore) + "." + statistic.substring(lastUnderscore + 1);
            val = userCache.get(candidate);
            if (val != null) return val;
        }
        int lastDot = statistic.lastIndexOf('.');
        if (lastDot >= 0) {
            String candidate = statistic.substring(0, lastDot) + "_" + statistic.substring(lastDot + 1);
            val = userCache.get(candidate);
            if (val != null) return val;
        }
        return null;
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
