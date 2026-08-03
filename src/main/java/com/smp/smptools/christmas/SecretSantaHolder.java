package com.smp.smptools.christmas;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class SecretSantaHolder implements InventoryHolder {
    private final UUID targetUUID;
    private Inventory inventory;

    public SecretSantaHolder(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, 27);
        }
        return inventory;
    }
}
