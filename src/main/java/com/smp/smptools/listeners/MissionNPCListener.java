package com.smp.smptools.listeners;

import com.smp.smptools.commands.MissionCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public class MissionNPCListener implements Listener {

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clickedEntity = event.getRightClicked();
        Player player = event.getPlayer();

        // Check if the entity has our custom PDC tag
        if (clickedEntity.getPersistentDataContainer().has(MissionCommand.MISSION_NPC_KEY, PersistentDataType.BYTE)) {
            // Cancel the default interaction (e.g., villager trading GUI)
            event.setCancelled(true);

            // Open the mission GUI for the player
            MissionGUIListener.openMissionGUI(player);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Check if the damaged entity is our mission NPC
        if (entity.getPersistentDataContainer().has(MissionCommand.MISSION_NPC_KEY, PersistentDataType.BYTE)) {
            // Cancel the damage event to make it fully invulnerable
            event.setCancelled(true);
        }
    }
}
