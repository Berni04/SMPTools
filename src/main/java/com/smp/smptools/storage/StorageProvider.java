package com.smp.smptools.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface defining persistence operations for SMPTools data.
 */
public interface StorageProvider {

    /**
     * Initializes connections and table/collection structures.
     */
    void init();

    /**
     * Shuts down storage connections.
     */
    void shutdown();

    /**
     * Saves a individual stat key/value for a player.
     */
    void saveStat(UUID uuid, String statKey, Object value);

    /**
     * Retrieves a stat for a player.
     */
    Object getStat(UUID uuid, String statKey, Object defaultValue);

    /**
     * Retrieves a long stat for a player.
     */
    long getLongStat(UUID uuid, String statKey, long defaultValue);

    /**
     * Gets all stats for a specific player.
     */
    Map<String, Object> getAllPlayerStats(UUID uuid);

    /**
     * Gets stats mapping for all players (UUID -> (statKey -> value)).
     */
    Map<UUID, Map<String, Object>> loadAllPlayerStats();

    /**
     * Clears all recorded statistics for a player.
     */
    void clearPlayerStats(UUID uuid);

    /**
     * Saves a player's equipped title/tag.
     */
    void savePlayerTitle(UUID uuid, String title);

    /**
     * Gets a player's equipped title/tag.
     */
    String getPlayerTitle(UUID uuid);

    /**
     * Removes a player's equipped title/tag.
     */
    void removePlayerTitle(UUID uuid);

    /**
     * Returns a map of player UUID strings to their equipped titles.
     */
    Map<String, String> getAllPlayerTitles();

    /**
     * Returns a leaderboard map (player name/UUID -> score) sorted descending for a specific stat.
     */
    Map<String, Long> getLeaderboardStats(String statPath);
}
