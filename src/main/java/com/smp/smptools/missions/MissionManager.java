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
    private File playerMissionsFile;
    private FileConfiguration playerMissionsConfig;

    public MissionManager(SMPTools plugin) {
        this.plugin = plugin;
        loadMissions();
        loadPlayerData();
        startAutoSaveTask();
    }

    private void startAutoSaveTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::savePlayerData, 6000L, 6000L); // Save every 5
                                                                                                   // minutes
    }

    private void loadMissions() {
        File missionsFile = new File(plugin.getDataFolder(), "missions.yml");
        if (!missionsFile.exists()) {
            plugin.saveResource("missions.yml", false);
        }
        FileConfiguration missionsConfig = YamlConfiguration.loadConfiguration(missionsFile);
        ConfigurationSection missionsSection = missionsConfig.getConfigurationSection("missions");
        if (missionsSection == null)
            return;

        for (String missionId : missionsSection.getKeys(false)) {
            String name = missionsSection.getString(missionId + ".name");
            String description = missionsSection.getString(missionId + ".description");
            MissionType type = MissionType.valueOf(missionsSection.getString(missionId + ".type"));
            String objective = missionsSection.getString(missionId + ".objective");
            int amount = missionsSection.getInt(missionId + ".amount");
            List<String> rewards = missionsSection.getStringList(missionId + ".rewards");
            List<String> prerequisites = missionsSection.getStringList(missionId + ".prerequisites");
            String category = missionsSection.getString(missionId + ".category", "NORMAL");

            Mission mission = new Mission(missionId, name, description, type, objective, amount, rewards, prerequisites,
                    category);
            this.missions.put(missionId, mission);
        }
        plugin.getLogger().info("Loaded " + missions.size() + " missions.");
    }

    private void loadPlayerData() {
        playerMissionsFile = new File(plugin.getDataFolder(), "player_missions.yml");
        if (!playerMissionsFile.exists()) {
            try {
                playerMissionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create player_missions.yml!");
            }
        }
        playerMissionsConfig = YamlConfiguration.loadConfiguration(playerMissionsFile);
    }

    public void savePlayerData() {
        for (Map.Entry<UUID, PlayerMissionData> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerMissionData data = entry.getValue();

            playerMissionsConfig.set(path + ".selectedQuestline", data.getSelectedQuestline());
            playerMissionsConfig.set(path + ".completed", data.getCompletedMissions());
            playerMissionsConfig.set(path + ".active", data.getActiveMissions());
            playerMissionsConfig.set(path + ".claimed", data.getClaimedMissions());

            // Save progress map
            ConfigurationSection progressSection = playerMissionsConfig.createSection(path + ".progress");
            for (Map.Entry<String, Integer> progressEntry : data.getMissionProgress().entrySet()) {
                progressSection.set(progressEntry.getKey(), progressEntry.getValue());
            }
        }

        try {
            playerMissionsConfig.save(playerMissionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player_missions.yml!");
        }
    }

    public PlayerMissionData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerMissionData data = new PlayerMissionData(uuid);
            String path = "players." + uuid.toString();

            if (playerMissionsConfig.contains(path)) {
                data.setSelectedQuestline(playerMissionsConfig.getString(path + ".selectedQuestline"));
                data.getCompletedMissions().addAll(playerMissionsConfig.getStringList(path + ".completed"));
                data.getActiveMissions().addAll(playerMissionsConfig.getStringList(path + ".active"));
                data.getClaimedMissions().addAll(playerMissionsConfig.getStringList(path + ".claimed"));

                ConfigurationSection progressSection = playerMissionsConfig.getConfigurationSection(path + ".progress");
                if (progressSection != null) {
                    for (String key : progressSection.getKeys(false)) {
                        data.getMissionProgress().put(key, progressSection.getInt(key));
                    }
                }
            }
            return data;
        });
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
        if (mission == null)
            return;

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
        private String selectedQuestline = null;

        public PlayerMissionData(UUID playerUUID) {
            this.playerUUID = playerUUID;
        }

        public Map<String, Integer> getMissionProgress() {
            return missionProgress;
        }

        public List<String> getCompletedMissions() {
            return completedMissions;
        }

        public List<String> getActiveMissions() {
            return activeMissions;
        }

        public List<String> getClaimedMissions() {
            return claimedMissions;
        } // Getter for claimed missions

        public String getSelectedQuestline() {
            return selectedQuestline;
        }

        public void setSelectedQuestline(String selectedQuestline) {
            this.selectedQuestline = selectedQuestline;
        }
    }
}
