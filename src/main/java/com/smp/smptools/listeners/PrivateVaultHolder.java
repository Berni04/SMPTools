package com.smp.smptools.listeners;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PrivateVaultHolder implements InventoryHolder {
    private Inventory inventory;
    private boolean decodeFailed = false;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isDecodeFailed() {
        return decodeFailed;
    }

    public void setDecodeFailed(boolean decodeFailed) {
        this.decodeFailed = decodeFailed;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
