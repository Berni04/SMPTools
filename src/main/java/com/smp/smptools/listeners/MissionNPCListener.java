package com.smp.smptools.listeners;

import com.smp.smptools.commands.MissionCommand;
import com.smp.smptools.managers.NPCManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import com.smp.smptools.SMPTools;

import com.smp.smptools.missions.MissionManager;
import org.bukkit.event.EventPriority;

public class MissionNPCListener implements Listener {

    private final SMPTools plugin;

    public MissionNPCListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clickedEntity = event.getRightClicked();
        Player player = event.getPlayer();

        // Check if the entity has our custom PDC tag (Quest Master)
        if (clickedEntity.getPersistentDataContainer().has(NPCManager.MISSION_NPC_KEY, PersistentDataType.BYTE)) {

            // Cancel the default interaction (e.g., villager trading GUI)
            event.setCancelled(true);

            // Determine category based on NPC type
            String category = "NORMAL";

            // Open the mission GUI for the player with the correct category
            MissionGUIListener.openMissionGUI(player, true, category);
        } else if (clickedEntity.getPersistentDataContainer().has(NPCManager.SANTA_NPC_KEY, PersistentDataType.BYTE)) {
            // Check if player has already selected a questline
            MissionManager.PlayerMissionData playerData = plugin.getMissionManager().getPlayerData(player);
            if (playerData.getSelectedQuestline() != null) {
                plugin.getLogger().info("Player " + player.getName() + " has questline "
                        + playerData.getSelectedQuestline() + ". Opening GUI directly.");
                // Player has a questline, so we bypass dialogue and open GUI directly
                event.setCancelled(true);
                MissionGUIListener.openMissionGUI(player, true, playerData.getSelectedQuestline());
            } else {
                plugin.getLogger().info("Player " + player.getName() + " has NO questline. Starting dialogue.");
            }
            // If no questline, we do NOT cancel. NPCListener will pick it up and start
            // dialogue.
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Check if the damaged entity is our mission NPC
        if (entity.getPersistentDataContainer().has(NPCManager.MISSION_NPC_KEY, PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(NPCManager.SANTA_NPC_KEY, PersistentDataType.BYTE)) {
            // Cancel the damage event to make it fully invulnerable
            event.setCancelled(true);
        }
    }
}
