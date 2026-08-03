package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.missions.Mission;
import com.smp.smptools.missions.MissionManager;
import com.smp.smptools.missions.MissionType;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.inventory.ItemStack;

public class MissionTrackerListener implements Listener {

    private final MissionManager missionManager;

    public MissionTrackerListener(SMPTools plugin) {
        this.missionManager = plugin.getMissionManager();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        updateMissionProgress(player, MissionType.BREAK_BLOCK, event.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            updateMissionProgress(player, MissionType.KILL_MOB, event.getEntityType().name(), 1);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack result = event.getInventory().getResult();
            if (result != null) {
                int amount = result.getAmount();
                if (event.isShiftClick()) {
                    // Simple approximation for shift-click crafting, usually accurate enough for
                    // missions
                    // A proper implementation would check max stack size and inventory space
                    // For now, we'll just count the result stack size * 1 (conservative) or try to
                    // calculate
                    // But to be safe and avoid exploits or complex logic, let's just count the
                    // result amount
                    // Actually, shift-clicking can craft many items at once.
                    // Let's stick to a simple implementation: count the result amount.
                    // If they craft 64 cookies at once, it counts as 64.
                    // Note: getRecipe().getResult() might be better but getInventory().getResult()
                    // is the actual item.

                    // However, calculating the exact amount for shift-click is complex in Bukkit.
                    // We will assume 1 recipe execution per event for safety or just the stack
                    // size.
                    // Let's use the stack size of the result.
                }
                updateMissionProgress(player, MissionType.CRAFT_ITEM, result.getType().name(), amount);
            }
        }
    }

    @EventHandler
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        Player player = event.getPlayer();
        Statistic statistic = event.getStatistic();
        // Calculate the difference to make it relative to when the mission started
        int amount = event.getNewValue() - event.getPreviousValue();
        updateMissionProgress(player, MissionType.STATISTIC, statistic.name(), amount);
    }

    private void updateMissionProgress(Player player, MissionType type, String objective, int amount) {
        MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);

        // Create a copy to avoid ConcurrentModificationException if we complete a
        // mission
        for (String missionId : new java.util.ArrayList<>(playerData.getActiveMissions())) {
            Mission mission = missionManager.getMission(missionId);
            if (mission == null || mission.getType() != type) {
                continue;
            }

            // Check if objective matches
            // For STATISTIC, the objective in yml (e.g. AVIATE_ONE_CM) should match the
            // statistic name
            if (mission.getObjective().equalsIgnoreCase(objective)) {
                int currentProgress = playerData.getMissionProgress().getOrDefault(missionId, 0);

                // Always add the amount (increment), never set absolute
                // This ensures all mission types track "since acceptance"
                int newProgress = currentProgress + amount;

                playerData.getMissionProgress().put(missionId, newProgress);

                // Notify player if they just completed it (or reached the goal)
                // We only notify once when they reach the goal
                if (currentProgress < mission.getAmount() && newProgress >= mission.getAmount()) {
                    missionManager.forceCompleteMission(player, missionId);
                    missionManager.savePlayerData(); // Save immediately
                    player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.completed", player, Map.of("mission_name", mission.getName())));

                    String npcName = "Quest Master";
                    if (mission.getCategory().toUpperCase().contains("CHRISTMAS")) {
                        npcName = "Santa Claus";
                    }

                    player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.return-to-npc", player, Map.of("npc", npcName)));

                    // Check for newly unlocked missions
                    for (Mission potentialMission : missionManager.getAllMissions().values()) {
                        // Skip if already completed or active
                        if (playerData.getCompletedMissions().contains(potentialMission.getId()) ||
                                playerData.getActiveMissions().contains(potentialMission.getId())) {
                            continue;
                        }

                        // Check if this mission depends on the one just completed
                        if (potentialMission.getPrerequisites() != null
                                && potentialMission.getPrerequisites().contains(missionId)) {
                            // Check if ALL prerequisites are met
                            boolean allMet = true;
                            for (String prereq : potentialMission.getPrerequisites()) {
                                if (!playerData.getCompletedMissions().contains(prereq)) {
                                    allMet = false;
                                    break;
                                }
                            }

                            if (allMet) {
                                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.mission-available", player, Map.of("mission_name", potentialMission.getName())));
                            }
                        }
                    }
                }
            }
        }
    }
}
