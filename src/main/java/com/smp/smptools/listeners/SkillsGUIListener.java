package com.smp.smptools.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SkillsGUIListener implements Listener {

    @EventHandler
    public void onSkillsGUIClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Your Skills")) {
            event.setCancelled(true);
        }
    }
}
