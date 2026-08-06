package com.smp.smptools.leaderboard;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final SMPTools plugin;
    private final Map<String, Map<String, Long>> cachedLeaderboards = new ConcurrentHashMap<>();
    private long lastCacheTime = 0;

    public LeaderboardManager(SMPTools plugin) {
        this.plugin = plugin;
        recalculateLeaderboards();
    }

    public Map<String, Long> getLeaderboard(String statPath) {
        long now = System.currentTimeMillis();
        // Cache for 5 minutes
        if (now - lastCacheTime > 300000 || !cachedLeaderboards.containsKey(statPath)) {
            recalculateLeaderboards();
        }
        return cachedLeaderboards.getOrDefault(statPath, Collections.emptyMap());
    }

    public void recalculateLeaderboards() {
        lastCacheTime = System.currentTimeMillis();

        if (plugin.getStorageManager() == null || plugin.getStorageManager().getProvider() == null) {
            return;
        }

        Runnable fetchTask = () -> {
            String[] statsToLoad = new String[]{
                    "blocks_broken", "blocks_placed", "playtime", "deaths", "player_kills",
                    "ores_mined.coal", "ores_mined.iron", "ores_mined.gold", "ores_mined.lapis",
                    "ores_mined.redstone", "ores_mined.diamond", "ores_mined.emerald"
            };

            Map<String, Map<String, Long>> newCache = new ConcurrentHashMap<>();
            for (String stat : statsToLoad) {
                Map<String, Long> statMap = plugin.getStorageManager().getProvider().getLeaderboardStats(stat);
                newCache.put(stat, sortLeaderboard(statMap));
            }
            cachedLeaderboards.putAll(newCache);
        };

        if (Bukkit.getServer() != null && Bukkit.isPrimaryThread() && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, fetchTask);
        } else {
            fetchTask.run();
        }
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
