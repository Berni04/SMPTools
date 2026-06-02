package com.smp.smptools.listeners;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder that carries the home name being confirmed for deletion.
 * Used by {@link HomesGUIListener} so the click handler can recover the
 * home name without parsing it out of the (configurable) inventory title.
 */
public final class HomeDeleteConfirmHolder implements InventoryHolder {

    private final String homeName;
    private Inventory inventory;

    public HomeDeleteConfirmHolder(String homeName) {
        this.homeName = homeName;
    }

    public String getHomeName() {
        return homeName;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
