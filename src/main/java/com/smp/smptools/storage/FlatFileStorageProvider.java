package com.smp.smptools.storage;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class FlatFileStorageProvider implements StorageProvider {

    private final SMPTools plugin;

    public FlatFileStorageProvider(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        plugin.getLogger().info("Storage provider set to FLATFILE (YAML).");
    }

    @Override
    public void shutdown() {
        plugin.saveStatsConfig();
        plugin.saveTagsConfig();
    }

    @Override
    public void saveStat(UUID uuid, String statKey, Object value) {
        plugin.getStatsConfig().set("stats." + uuid + "." + statKey, value);
        plugin.saveStatsConfig();
    }

    @Override
    public Object getStat(UUID uuid, String statKey, Object defaultValue) {
        Object val = plugin.getStatsConfig().get("stats." + uuid + "." + statKey);
        return StorageProvider.parseCanonicalValue(val, defaultValue);
    }

    @Override
    public long getLongStat(UUID uuid, String statKey, long defaultValue) {
        Object val = getStat(uuid, statKey, defaultValue);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }


    @Override
    public Map<String, Object> getAllPlayerStats(UUID uuid) {
        Map<String, Object> map = new HashMap<>();
        ConfigurationSection section = plugin.getStatsConfig().getConfigurationSection("stats." + uuid);
        if (section != null) {
            for (String key : section.getKeys(true)) {
                if (!section.isConfigurationSection(key)) {
                    map.put(key, section.get(key));
                }
            }
        }
        return map;
    }

    @Override
    public Map<UUID, Map<String, Object>> loadAllPlayerStats() {
        Map<UUID, Map<String, Object>> allStats = new HashMap<>();
        ConfigurationSection statsSection = plugin.getStatsConfig().getConfigurationSection("stats");
        if (statsSection != null) {
            for (String uuidStr : statsSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    allStats.put(uuid, getAllPlayerStats(uuid));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return allStats;
    }

    @Override
    public void clearPlayerStats(UUID uuid) {
        String activeTrail = plugin.getStatsConfig().getString("stats." + uuid + ".active_trail");
        plugin.getStatsConfig().set("stats." + uuid, null);
        if (activeTrail != null) {
            plugin.getStatsConfig().set("stats." + uuid + ".active_trail", activeTrail);
        }
        plugin.saveStatsConfig();
    }

    @Override
    public void savePlayerTitle(UUID uuid, String title) {
        plugin.getTagsConfig().set("player-titles." + uuid, title);
        plugin.saveTagsConfig();
    }

    @Override
    public String getPlayerTitle(UUID uuid) {
        return plugin.getTagsConfig().getString("player-titles." + uuid);
    }

    @Override
    public void removePlayerTitle(UUID uuid) {
        plugin.getTagsConfig().set("player-titles." + uuid, null);
        plugin.saveTagsConfig();
    }

    @Override
    public Map<String, String> getAllPlayerTitles() {
        Map<String, String> titles = new HashMap<>();
        ConfigurationSection section = plugin.getTagsConfig().getConfigurationSection("player-titles");
        if (section != null) {
            for (String uuidStr : section.getKeys(false)) {
                titles.put(uuidStr, section.getString(uuidStr));
            }
        }
        return titles;
    }

    @Override
    public Map<String, Long> getLeaderboardStats(String statPath) {
        Map<String, Long> leaderboard = new LinkedHashMap<>();
        ConfigurationSection statsSection = plugin.getStatsConfig().getConfigurationSection("stats");
        if (statsSection == null) return leaderboard;

        Map<String, Long> rawMap = new HashMap<>();
        for (String uuidStr : statsSection.getKeys(false)) {
            ConfigurationSection playerSection = statsSection.getConfigurationSection(uuidStr);
            if (playerSection != null && playerSection.contains(statPath)) {
                try {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                    String name = player.getName() != null ? player.getName() : "Unknown";
                    long val = playerSection.getLong(statPath, 0);
                    rawMap.put(name, val);
                } catch (Exception ignored) {}
            }
        }

        rawMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(e -> leaderboard.put(e.getKey(), e.getValue()));

        return leaderboard;
    }
}
