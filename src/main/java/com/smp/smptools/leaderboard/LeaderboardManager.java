package com.smp.smptools.leaderboard;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private static final String[] STATS_TO_LOAD = new String[]{
            "blocks_broken", "blocks_placed", "playtime", "deaths", "player_kills",
            "ores_mined.coal", "ores_mined.iron", "ores_mined.gold", "ores_mined.lapis",
            "ores_mined.redstone", "ores_mined.diamond", "ores_mined.emerald"
    };

    private final SMPTools plugin;
    private final Map<String, Map<String, Long>> cachedLeaderboards = new ConcurrentHashMap<>();
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private long lastCacheTime = 0;

    public LeaderboardManager(SMPTools plugin) {
        this.plugin = plugin;
        boolean enabled = plugin != null && plugin.getConfig() != null
                && plugin.getConfig().getBoolean("features.leaderboard.enabled", true);
        if (enabled && Bukkit.getServer() != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::recalculateLeaderboards);
        }
    }

    public Map<String, Long> getLeaderboard(String statPath) {
        long now = System.currentTimeMillis();
        // Cache for 5 minutes
        if (now - lastCacheTime > 300000 || !cachedLeaderboards.containsKey(statPath)) {
            if (Bukkit.getServer() != null && plugin != null && plugin.isEnabled()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, this::recalculateLeaderboards);
            } else {
                recalculateLeaderboards();
            }
        }
        return cachedLeaderboards.getOrDefault(statPath, Collections.emptyMap());
    }

    public void recalculateLeaderboards() {
        if (plugin == null || plugin.getStorageManager() == null || plugin.getStorageManager().getProvider() == null) {
            return;
        }

        if (!isRefreshing.compareAndSet(false, true)) {
            return; // Coalesce concurrent refresh calls
        }

        Runnable task = () -> {
            try {
                Map<String, Map<String, Long>> rawSnapshots = snapshotRawStats();
                Map<String, Map<String, Long>> sorted = sortSnapshots(rawSnapshots);
                cachedLeaderboards.putAll(sorted);
                lastCacheTime = System.currentTimeMillis();
            } finally {
                isRefreshing.set(false);
            }
        };

        if (Bukkit.getServer() != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            task.run();
        }
    }

    private Map<String, Map<String, Long>> snapshotRawStats() {
        if (plugin == null || plugin.getStorageManager() == null || plugin.getStorageManager().getProvider() == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Long>> rawSnapshots = new HashMap<>();
        for (String stat : STATS_TO_LOAD) {
            String dbKey = getDbStatKey(stat);
            Map<String, Long> statMap = plugin.getStorageManager().getProvider().getLeaderboardStats(dbKey);
            rawSnapshots.put(stat, statMap != null ? new HashMap<>(statMap) : Collections.emptyMap());
        }
        return rawSnapshots;
    }

    private Map<String, Map<String, Long>> sortSnapshots(Map<String, Map<String, Long>> rawSnapshots) {
        Map<String, Map<String, Long>> sortedCache = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String, Long>> entry : rawSnapshots.entrySet()) {
            sortedCache.put(entry.getKey(), sortLeaderboard(entry.getValue()));
        }
        return sortedCache;
    }

    private String getDbStatKey(String stat) {
        if ("playtime".equals(stat)) {
            return "playtime_minutes";
        }
        if ("deaths".equals(stat)) {
            return "deaths_total";
        }
        return stat;
    }

    private Map<String, Long> sortLeaderboard(Map<String, Long> unsortedMap) {
        if (unsortedMap == null) return Collections.emptyMap();
        return unsortedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}

