package com.smp.smptools.listeners;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder identifying a leaderboard stat GUI (e.g. "Top 10 - Kills").
 * Carries the stat key so the click handler can recover it without
 * matching the (configurable) inventory title prefix.
 */
public final class LeaderboardStatHolder implements InventoryHolder {

    private final String statKey;
    private Inventory inventory;

    public LeaderboardStatHolder(String statKey) {
        this.statKey = statKey;
    }

    public String getStatKey() {
        return statKey;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
