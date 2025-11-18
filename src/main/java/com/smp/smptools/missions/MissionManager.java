package com.smp.smptools.missions;

import com.smp.smptools.SMPTools;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MissionManager {

    private final SMPTools plugin;
    private final Map<String, Mission> missions = new HashMap<>();
    private final Map<UUID, PlayerMissionData> playerData = new HashMap<>();

    public MissionManager(SMPTools plugin) {
        this.plugin = plugin;
        loadMissions();
    }

    private void loadMissions() {
        File missionsFile = new File(plugin.getDataFolder(), "missions.yml");
        if (!missionsFile.exists()) {
            plugin.saveResource("missions.yml", false);
        }
        FileConfiguration missionsConfig = YamlConfiguration.loadConfiguration(missionsFile);
        ConfigurationSection missionsSection = missionsConfig.getConfigurationSection("missions");
        if (missionsSection == null) return;

        for (String missionId : missionsSection.getKeys(false)) {
            String name = missionsSection.getString(missionId + ".name");
            String description = missionsSection.getString(missionId + ".description");
            MissionType type = MissionType.valueOf(missionsSection.getString(missionId + ".type"));
            String objective = missionsSection.getString(missionId + ".objective");
            int amount = missionsSection.getInt(missionId + ".amount");
            List<String> rewards = missionsSection.getStringList(missionId + ".rewards");
            List<String> prerequisites = missionsSection.getStringList(missionId + ".prerequisites");

            Mission mission = new Mission(missionId, name, description, type, objective, amount, rewards, prerequisites);
            this.missions.put(missionId, mission);
        }
        plugin.getLogger().info("Loaded " + missions.size() + " missions.");
    }

    public PlayerMissionData getPlayerData(Player player) {
        // For now, we'll just use an in-memory map.
        // A full implementation would load from a file.
        return playerData.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerMissionData(uuid));
    }

    public Mission getMission(String missionId) {
        return missions.get(missionId);
    }

    public Map<String, Mission> getAllMissions() {
        return missions;
    }

    public void forceCompleteMission(Player player, String missionId) {
        PlayerMissionData data = getPlayerData(player);
        Mission mission = getMission(missionId);
        if (mission == null) return;

        if (!data.getCompletedMissions().contains(missionId)) {
            data.getCompletedMissions().add(missionId);
        }
        data.getActiveMissions().remove(missionId);
        // Set progress to the goal to ensure it's recognized as complete
        data.getMissionProgress().put(missionId, mission.getAmount());
    }

    public void resetMission(Player player, String missionId) {
        PlayerMissionData data = getPlayerData(player);
        data.getCompletedMissions().remove(missionId);
        data.getActiveMissions().remove(missionId);
        data.getMissionProgress().remove(missionId);
        data.getClaimedMissions().remove(missionId); // Also remove from claimed missions
    }

    // Placeholder class for player data
    public static class PlayerMissionData {
        private final UUID playerUUID;
        private final Map<String, Integer> missionProgress = new HashMap<>();
        private final List<String> completedMissions = new ArrayList<>();
        private final List<String> activeMissions = new ArrayList<>();
        private final List<String> claimedMissions = new ArrayList<>(); // New list for claimed missions

        public PlayerMissionData(UUID playerUUID) {
            this.playerUUID = playerUUID;
        }

        public Map<String, Integer> getMissionProgress() { return missionProgress; }
        public List<String> getCompletedMissions() { return completedMissions; }
        public List<String> getActiveMissions() { return activeMissions; }
        public List<String> getClaimedMissions() { return claimedMissions; } // Getter for claimed missions
    }
}
