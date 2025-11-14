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

        ConfigurationSection statsSection = plugin.getStatsConfig().getConfigurationSection("stats");
        if (statsSection == null) {
            return;
        }

        Map<String, Long> blocksBroken = new LinkedHashMap<>();
        Map<String, Long> blocksPlaced = new LinkedHashMap<>();
        Map<String, Long> playtime = new LinkedHashMap<>();
        Map<String, Long> deaths = new LinkedHashMap<>();
        Map<String, Long> playerKills = new LinkedHashMap<>();
        // New maps for individual ore types
        Map<String, Long> oresMinedCoal = new LinkedHashMap<>();
        Map<String, Long> oresMinedIron = new LinkedHashMap<>();
        Map<String, Long> oresMinedGold = new LinkedHashMap<>();
        Map<String, Long> oresMinedLapis = new LinkedHashMap<>();
        Map<String, Long> oresMinedRedstone = new LinkedHashMap<>();
        Map<String, Long> oresMinedDiamond = new LinkedHashMap<>();
        Map<String, Long> oresMinedEmerald = new LinkedHashMap<>();

        for (String uuid : statsSection.getKeys(false)) {
            ConfigurationSection playerSection = statsSection.getConfigurationSection(uuid);
            if (playerSection != null) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
                String name = player.getName() != null ? player.getName() : "Unknown";

                blocksBroken.put(name, playerSection.getLong("blocks_broken", 0));
                blocksPlaced.put(name, playerSection.getLong("blocks_placed", 0));
                playtime.put(name, playerSection.getLong("playtime_minutes", 0));
                deaths.put(name, playerSection.getLong("deaths_total", 0));
                playerKills.put(name, playerSection.getLong("player_kills", 0));

                // Extract individual ore stats
                ConfigurationSection oresMinedSection = playerSection.getConfigurationSection("ores_mined");
                if (oresMinedSection != null) {
                    oresMinedCoal.put(name, oresMinedSection.getLong("coal", 0));
                    oresMinedIron.put(name, oresMinedSection.getLong("iron", 0));
                    oresMinedGold.put(name, oresMinedSection.getLong("gold", 0));
                    oresMinedLapis.put(name, oresMinedSection.getLong("lapis", 0));
                    oresMinedRedstone.put(name, oresMinedSection.getLong("redstone", 0));
                    oresMinedDiamond.put(name, oresMinedSection.getLong("diamond", 0));
                    oresMinedEmerald.put(name, oresMinedSection.getLong("emerald", 0));
                }
            }
        }

        cachedLeaderboards.put("blocks_broken", sortLeaderboard(blocksBroken));
        cachedLeaderboards.put("blocks_placed", sortLeaderboard(blocksPlaced));
        cachedLeaderboards.put("playtime", sortLeaderboard(playtime));
        cachedLeaderboards.put("deaths", sortLeaderboard(deaths));
        cachedLeaderboards.put("player_kills", sortLeaderboard(playerKills));
        // Add new ore leaderboards
        cachedLeaderboards.put("ores_mined.coal", sortLeaderboard(oresMinedCoal));
        cachedLeaderboards.put("ores_mined.iron", sortLeaderboard(oresMinedIron));
        cachedLeaderboards.put("ores_mined.gold", sortLeaderboard(oresMinedGold));
        cachedLeaderboards.put("ores_mined.lapis", sortLeaderboard(oresMinedLapis));
        cachedLeaderboards.put("ores_mined.redstone", sortLeaderboard(oresMinedRedstone));
        cachedLeaderboards.put("ores_mined.diamond", sortLeaderboard(oresMinedDiamond));
        cachedLeaderboards.put("ores_mined.emerald", sortLeaderboard(oresMinedEmerald));
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
