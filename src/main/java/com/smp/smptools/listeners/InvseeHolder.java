package com.smp.smptools.listeners;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * InventoryHolder identifying an invsee GUI. Carries the target player's
 * UUID so the click handler can recover it without matching the
 * (configurable) inventory title prefix.
 */
public final class InvseeHolder implements InventoryHolder {

    private final java.util.UUID targetUuid;
    private Inventory inventory;

    public InvseeHolder(java.util.UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public java.util.UUID getTargetUuid() {
        return targetUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
