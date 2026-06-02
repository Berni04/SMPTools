package com.smp.smptools.listeners;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder identifying the leaderboard hub GUI (the menu of stats).
 * Used by {@link LeaderboardGUIListener} so the click handler can recognise
 * the hub without matching the (configurable) inventory title.
 */
public final class LeaderboardHubHolder implements InventoryHolder {

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
