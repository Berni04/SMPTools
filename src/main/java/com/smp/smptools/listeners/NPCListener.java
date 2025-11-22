package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.managers.NPCManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class NPCListener implements Listener {

    private final SMPTools plugin;

    public NPCListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Check if it's one of our NPCs
        if (entity.getPersistentDataContainer().has(NPCManager.DIALOGUE_ID_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            String dialogueId = entity.getPersistentDataContainer().get(NPCManager.DIALOGUE_ID_KEY,
                    PersistentDataType.STRING);
            handleDialogue(player, entity, dialogueId);
        } else if (entity.getPersistentDataContainer().has(NPCManager.STORY_NPC_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handleDialogue(player, entity, "story_npc");
        } else if (entity.getPersistentDataContainer().has(NPCManager.SANTA_NPC_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handleDialogue(player, entity, "santa");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (isNPC(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (isNPC(entity)) {
            event.setCancelled(true);
        }
    }

    private boolean isNPC(Entity entity) {
        return entity.getPersistentDataContainer().has(NPCManager.NPC_ID_KEY, PersistentDataType.STRING) ||
                entity.getPersistentDataContainer().has(NPCManager.STORY_NPC_KEY, PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(NPCManager.SANTA_NPC_KEY, PersistentDataType.BYTE) ||
                entity.getPersistentDataContainer().has(NPCManager.MISSION_NPC_KEY, PersistentDataType.BYTE);
    }

    private void handleDialogue(Player player, Entity npc, String dialogueId) {
        if (plugin.getDialogueManager().isInDialogue(player)) {
            plugin.getDialogueManager().advanceDialogue(player, npc);
        } else {
            plugin.getDialogueManager().startDialogue(player, npc, dialogueId);
        }
    }
}
