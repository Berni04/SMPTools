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
import java.util.concurrent.ConcurrentHashMap;

public class MissionManager {

    private final SMPTools plugin;
    private final Map<String, Mission> missions = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerMissionData> playerData = new ConcurrentHashMap<>();
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
            String typeStr = missionsSection.getString(missionId + ".type");
            if (typeStr == null) {
                plugin.getLogger().warning("Missing type for mission: " + missionId);
                continue;
            }

            MissionType type;
            try {
                type = MissionType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid mission type '" + typeStr + "' for mission: " + missionId);
                continue;
            }

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

    private void serializePlayerMissionData(UUID uuid, PlayerMissionData data) {
        String path = "players." + uuid.toString();
        playerMissionsConfig.set(path + ".selectedQuestline", data.getSelectedQuestline());
        playerMissionsConfig.set(path + ".completed", data.getCompletedMissions());
        playerMissionsConfig.set(path + ".active", data.getActiveMissions());
        playerMissionsConfig.set(path + ".claimed", data.getClaimedMissions());
        playerMissionsConfig.set(path + ".pendingRewards", data.getPendingRewards().isEmpty() ? null : data.getPendingRewards());

        // Save progress map
        ConfigurationSection progressSection = playerMissionsConfig.createSection(path + ".progress");
        for (Map.Entry<String, Integer> progressEntry : data.getMissionProgress().entrySet()) {
            progressSection.set(progressEntry.getKey(), progressEntry.getValue());
        }
    }

    public synchronized boolean savePlayerData() {
        for (Map.Entry<UUID, PlayerMissionData> entry : playerData.entrySet()) {
            serializePlayerMissionData(entry.getKey(), entry.getValue());
        }

        try {
            com.smp.smptools.utils.AtomicFileWriter.save(playerMissionsConfig, playerMissionsFile);
            return true;
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().severe("Could not save player_missions.yml: " + e.getMessage());
            }
            return false;
        }
    }

    public synchronized boolean saveSinglePlayerData(UUID uuid) {
        if (uuid == null) return false;
        PlayerMissionData data = playerData.get(uuid);
        if (data == null) return false;
        serializePlayerMissionData(uuid, data);
        try {
            com.smp.smptools.utils.AtomicFileWriter.save(playerMissionsConfig, playerMissionsFile);
            return true;
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().severe("Could not save player_missions.yml for " + uuid + ": " + e.getMessage());
            }
            return false;
        }
    }

    public enum ClaimResult {
        ALL_DELIVERED,
        PARTIALLY_PENDING,
        DROPPED_UNEXECUTABLE,
        PARTIALLY_PENDING_AND_DROPPED,
        NOTHING_TO_CLAIM
    }

    public synchronized ClaimResult claimPendingMissionRewards(Player player) {
        if (player == null || !player.isOnline()) return ClaimResult.NOTHING_TO_CLAIM;
        PlayerMissionData data = getPlayerData(player);
        if (data.getPendingRewards().isEmpty()) return ClaimResult.NOTHING_TO_CLAIM;

        List<String> pending = new ArrayList<>(data.getPendingRewards());
        boolean anyDelivered = false;
        int droppedCount = 0;

        for (String reward : pending) {
            List<String> snapshotBefore = new ArrayList<>(data.getPendingRewards());
            String baseReward = reward != null ? reward.trim() : "";
            int retryCount = 0;
            if (reward != null) {
                int retryIndex = baseReward.lastIndexOf("#retry:");
                if (retryIndex != -1) {
                    String suffix = baseReward.substring(retryIndex + 7).trim();
                    baseReward = baseReward.substring(0, retryIndex).trim();
                    try {
                        retryCount = Integer.parseInt(suffix);
                        if (retryCount < 0) {
                            retryCount = 3;
                        }
                    } catch (NumberFormatException e) {
                        retryCount = 3;
                    }
                }
            }

            if (!RewardManager.isValidReward(baseReward)) {
                if (plugin != null) {
                    plugin.getLogger().warning("Discarding unparseable/malformed pending mission reward '" + reward + "' for " + player.getName());
                }
                droppedCount++;
                data.getPendingRewards().remove(reward);
                if (!saveSinglePlayerData(player.getUniqueId())) {
                    data.getPendingRewards().clear();
                    data.getPendingRewards().addAll(snapshotBefore);
                    break;
                }
                continue;
            }

            // Persist removal from pending queue BEFORE delivering reward to prevent duplicates on crash or save failure
            data.getPendingRewards().remove(reward);
            if (!saveSinglePlayerData(player.getUniqueId())) {
                if (plugin != null) {
                    plugin.getLogger().severe("Failed to persist pending mission reward removal for " + player.getName() + ", aborting claim execution.");
                }
                data.getPendingRewards().clear();
                data.getPendingRewards().addAll(snapshotBefore);
                break;
            }

            boolean delivered = false;
            try {
                delivered = RewardManager.giveReward(player, baseReward);
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("Failed to deliver pending mission reward '" + reward + "' to " + player.getName() + ": " + e.getMessage());
                }
            }

            if (delivered) {
                anyDelivered = true;
            } else {
                int nextRetry = retryCount + 1;
                if (nextRetry >= 3) {
                    droppedCount++;
                    if (plugin != null) {
                        plugin.getLogger().severe("Permanently dropping unexecutable pending mission reward '" + baseReward + "' for " + player.getName() + " after 3 failed attempts.");
                    }
                } else {
                    data.getPendingRewards().add(baseReward + "#retry:" + nextRetry);
                    if (plugin != null) {
                        plugin.getLogger().warning("Transient delivery failure for pending mission reward '" + baseReward + "' for " + player.getName() + " (attempt " + nextRetry + "/3), retaining for retry.");
                    }
                    if (!saveSinglePlayerData(player.getUniqueId())) {
                        if (plugin != null) {
                            plugin.getLogger().severe("Failed to persist retry state for pending mission reward for " + player.getName() + ", aborting remaining queue.");
                        }
                        data.getPendingRewards().clear();
                        data.getPendingRewards().addAll(snapshotBefore);
                        saveSinglePlayerData(player.getUniqueId());
                        break;
                    }
                }
            }
        }

        if (anyDelivered) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gold><b>[MISSIONS]</b></gold> <green>You received pending mission rewards!</green>"));
        }

        boolean queueHasPending = !data.getPendingRewards().isEmpty();
        if (queueHasPending && droppedCount > 0) {
            return ClaimResult.PARTIALLY_PENDING_AND_DROPPED;
        } else if (queueHasPending) {
            return ClaimResult.PARTIALLY_PENDING;
        } else if (droppedCount > 0) {
            return ClaimResult.DROPPED_UNEXECUTABLE;
        } else {
            return ClaimResult.ALL_DELIVERED;
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
                data.getPendingRewards().addAll(playerMissionsConfig.getStringList(path + ".pendingRewards"));

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
        private final List<String> pendingRewards = new ArrayList<>();
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

        public List<String> getPendingRewards() {
            return pendingRewards;
        }

        public String getSelectedQuestline() {
            return selectedQuestline;
        }

        public void setSelectedQuestline(String selectedQuestline) {
            this.selectedQuestline = selectedQuestline;
        }
    }
}
