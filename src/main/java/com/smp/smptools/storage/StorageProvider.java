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

    /**
     * Converts a raw stored value into a canonical value matching the type of {@code defaultValue}.
     */
    static Object parseCanonicalValue(Object rawVal, Object defaultValue) {
        if (rawVal == null) {
            return defaultValue;
        }
        if (defaultValue == null) {
            if (rawVal instanceof Number || rawVal instanceof Boolean) {
                return rawVal;
            }
            String str = rawVal.toString();
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored1) {
                try {
                    return Double.parseDouble(str);
                } catch (NumberFormatException ignored2) {
                    if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
                        return Boolean.parseBoolean(str);
                    }
                    return str;
                }
            }
        }

        String str = rawVal.toString();
        if (defaultValue instanceof Integer) {
            if (rawVal instanceof Number) return ((Number) rawVal).intValue();
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (defaultValue instanceof Long) {
            if (rawVal instanceof Number) return ((Number) rawVal).longValue();
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                try {
                    return (long) Double.parseDouble(str);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        } else if (defaultValue instanceof Double) {
            if (rawVal instanceof Number) return ((Number) rawVal).doubleValue();
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (defaultValue instanceof Float) {
            if (rawVal instanceof Number) return ((Number) rawVal).floatValue();
            try {
                return Float.parseFloat(str);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else if (defaultValue instanceof Boolean) {
            if (rawVal instanceof Boolean) return rawVal;
            if (str.equalsIgnoreCase("true")) {
                return Boolean.TRUE;
            } else if (str.equalsIgnoreCase("false")) {
                return Boolean.FALSE;
            }
            return defaultValue;
        } else if (defaultValue instanceof String) {
            return str;
        }
        return rawVal;
    }
}

