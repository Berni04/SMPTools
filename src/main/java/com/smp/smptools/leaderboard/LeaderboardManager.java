package com.smp.smptools.leaderboard;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final SMPTools plugin;
    private final Map<String, Map<String, Long>> cachedLeaderboards = new LinkedHashMap<>();
    private long lastCacheTime = 0;

    public LeaderboardManager(SMPTools plugin) {
        this.plugin = plugin;
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
        cachedLeaderboards.clear();

        if (plugin.getStorageManager() == null || plugin.getStorageManager().getProvider() == null) {
            return;
        }

        String[] statsToLoad = new String[]{
                "blocks_broken", "blocks_placed", "playtime_minutes", "deaths_total", "player_kills",
                "ores_mined.coal", "ores_mined.iron", "ores_mined.gold", "ores_mined.lapis",
                "ores_mined.redstone", "ores_mined.diamond", "ores_mined.emerald"
        };

        for (String stat : statsToLoad) {
            Map<String, Long> statMap = plugin.getStorageManager().getProvider().getLeaderboardStats(stat);
            cachedLeaderboards.put(stat, sortLeaderboard(statMap));
        }
    }

    private Map<String, Long> sortLeaderboard(Map<String, Long> unsortedMap) {
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
