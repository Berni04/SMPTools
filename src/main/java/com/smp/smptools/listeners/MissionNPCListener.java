package com.smp.smptools.listeners;

import com.smp.smptools.commands.MissionCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import com.smp.smptools.SMPTools;
import com.smp.smptools.missions.MissionManager;

public class MissionNPCListener implements Listener {

    private final SMPTools plugin;

    public MissionNPCListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clickedEntity = event.getRightClicked();
        Player player = event.getPlayer();

        // Check if the entity has our custom PDC tag (Quest Master OR Santa)
        if (clickedEntity.getPersistentDataContainer().has(MissionCommand.MISSION_NPC_KEY, PersistentDataType.BYTE) ||
                clickedEntity.getPersistentDataContainer().has(MissionCommand.SANTA_NPC_KEY, PersistentDataType.BYTE)) {

            // Cancel the default interaction (e.g., villager trading GUI)
            event.setCancelled(true);

            // Determine category based on NPC type
            String category = "NORMAL";
            if (clickedEntity.getPersistentDataContainer().has(MissionCommand.SANTA_NPC_KEY, PersistentDataType.BYTE)) {
                MissionManager.PlayerMissionData playerData = plugin.getMissionManager().getPlayerData(player);
                String selectedQuestline = playerData.getSelectedQuestline();

                if (selectedQuestline != null) {
                    category = selectedQuestline;
                } else {
                    MissionGUIListener.openQuestlineSelectionGUI(player);
                    return;
                }
            }

            // Open the mission GUI for the player with the correct category
            MissionGUIListener.openMissionGUI(player, true, category);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Check if the damaged entity is our mission NPC
        if (entity.getPersistentDataContainer().has(MissionCommand.MISSION_NPC_KEY, PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(MissionCommand.SANTA_NPC_KEY, PersistentDataType.BYTE)) {
            // Cancel the damage event to make it fully invulnerable
            event.setCancelled(true);
        }
    }
}
